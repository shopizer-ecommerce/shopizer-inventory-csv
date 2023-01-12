package com.shopizer.inventory.csv.in.category;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;



public class CategoryImport {
	
	//private String FILE_NAME = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/category-loader.csv";
	//private String FILE_NAME =   "/Users/carlsamson/Documents/csti/projects-proposals/rufina/xls/category-loader.csv";
	private String FILE_NAME = "/Users/carlsamson/Documents/dev/mydidas-category.csv";
	//private String endPoint = "http://localhost:8080/api/v1/private/category?store=";
	private String endPoint = "https://demo-api.shopizer.com/api/v1/private/category?store=";
	
	
	private String langs[] = {"en"};
	
	private static final String MERCHANT = "DEFAULT";
	private boolean DRY_RUN = false;
	
	public static void main(String[] args) {
		
		CategoryImport categoryImport = new CategoryImport();
		try {
			categoryImport.importCategory();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void importCategory() throws Exception {
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');
	
		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.ISO_8859_1));
		
		//new FileReader(fileName)
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);

		Map<String,PersistableCategory> categoryMap = new HashMap<String,PersistableCategory>();

		int i = 0;
		//for (CSVRecord record : records) {
		for(CSVRecord record : parser){

			if(StringUtils.isBlank(record.get("code"))) {
				continue;
			}
			
			//System.out.println(this.codeGen(record.get("code")));
			//continue;

			//core properties
			PersistableCategory category = new PersistableCategory();
			category.setCode(record.get("code"));
			category.setSortOrder(Integer.parseInt(record.get("position")));
			category.setVisible(Integer.parseInt(record.get("visible"))==1?true:false);
			
			List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();
			
			
			
			for(int langLenth=0;langLenth<langs.length;langLenth++) {
				
				
				String lang = langs[langLenth];
				
				CategoryDescription description = new CategoryDescription();
				description.setLanguage(lang);
				
				
				description.setTitle(cleanup(record.get("name_" + lang)));
				description.setName(cleanup(record.get("name_" + lang)));
				if(record.isSet("description_" + lang)) {
					description.setDescription(cleanup(record.get("description_" + lang)));
				}

				description.setDescription(description.getName());
				//path - name
				
				StringBuilder path = new StringBuilder();
				String prefix = record.get("path_" + lang);
				if(!StringUtils.isBlank(prefix)) {
					path.append(prefix).append("-");
				}
				path.append(minimalFriendlyUrlCreator(description.getName()));
				
				description.setFriendlyUrl(path.toString().toLowerCase());
				

				descriptions.add(description);
				
			}

			category.setDescriptions(descriptions);
				
			categoryMap.put(category.getCode(), category);
			
			if(!StringUtils.isBlank(record.get("parent"))) {
				PersistableCategory parent = categoryMap.get(record.get("parent"));
				if(parent!=null) {
					Category parentCategory = new Category();
					parentCategory.setCode(parent.getCode());
					category.setParent(parentCategory);
					parent.getChildren().add(category);
				}
			}
			
			
			
			i++;//rows

		}
		
		HttpHeaders httpHeader = getHeader();
		
		//now save each category
		for(PersistableCategory category : categoryMap.values()) {
			
			if(category.getParent()==null) {//only root category
			
				ObjectMapper objectMapper = new ObjectMapper();
				String json = objectMapper.writeValueAsString(category);

				System.out.println("-----------------------");
				System.out.println(json);
				
				
				if(!DRY_RUN) {
					HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
		
					//post to create category web service
					ResponseEntity response = restTemplate.postForEntity(endPoint + MERCHANT, entity, PersistableCategory.class);
					PersistableCategory cat = (PersistableCategory) response.getBody();
				}
				
			}
			
		}
		
     
		System.out.println("------------------------------------");
		System.out.println("Category import done DRY_RUN [" + DRY_RUN + "]");
		System.out.println("------------------------------------");
		
	}
	
	
	/**
	 * TODO COMMON Utility
	 */
	private String cleanup(String field) {
		//cleanup unwelcome other charset characters
		return field.replaceAll("�", "");
	}
	
	public String minimalFriendlyUrlCreator(String productName) {
		productName = productName.toLowerCase();
		productName = productName.replace("  ", " ");
		
		//remove accents
		productName = Normalizer.normalize(productName, Normalizer.Form.NFD);
		productName = productName.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

		productName = productName.replace(" ", "-");
		return productName;
	}
	/**
	 * 
	 */
	
	
	private HttpHeaders getHeader(){
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		//MediaType.APPLICATION_JSON //for application/json
		headers.setContentType(mediaType);
		//Basic Authentication
		String authorisation = "admin@shopizer.com" + ":" + "password";
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}
	
	public String codeGen(String name) {
		name = name.toLowerCase();
		name = name.replace("  ", " ");
		
		//remove accents
		name = Normalizer.normalize(name, Normalizer.Form.NFD);
		name = name.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

		name = name.replace(" ", "-");
		return name;
	}

}
