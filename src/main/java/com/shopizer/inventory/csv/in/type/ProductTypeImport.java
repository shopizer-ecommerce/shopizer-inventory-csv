package com.shopizer.inventory.csv.in.type;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import com.salesmanager.shop.model.catalog.product.type.PersistableProductType;
import com.salesmanager.shop.model.catalog.product.type.ProductTypeDescription;

public class ProductTypeImport {

	private String FILE_NAME =   "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventory-xls/shopizer-inventory-csv/src/main/resources/types-loader.csv";
	private String endPoint = "http://localhost:8080/api/v1/private/products/type?store=";
	
	private static final String ADMIN_NAME = "admin@shopizer.com";
	private static final String ADMIN_PASSWORD = "password";
	
	private static final String MERCHANT = "DEFAULT";
	
	public static void main(String[] args) {
		
		ProductTypeImport manufacturerImport = new ProductTypeImport();
		try {
			manufacturerImport.process();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void process() throws Exception {
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');

		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.ISO_8859_1));
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);
		
		Map<String, Integer> headerMap = parser.getHeaderMap();

		HttpHeaders httpHeader = getHeader();

		int i = 0;
		
		for(CSVRecord record : parser){

			if(StringUtils.isBlank(record.get("type_en"))) {
				continue;
			}
			
			/**
			 * temp code generate friendly code
			 */
			//System.out.println(this.codeGen(record.get("type_en")));
			//continue;

			PersistableProductType type = new PersistableProductType();
			type.setCode(record.get("code"));
			type.setAllowAddToCart(true);
			type.setVisible(true);
			type.setId(null);


			List<ProductTypeDescription> descriptions = new ArrayList<ProductTypeDescription>();
			
			//add english description
			ProductTypeDescription description = new ProductTypeDescription();
			description.setLanguage("en");
			description.setName(record.get("type_en"));
			description.setDescription(record.get("type_en"));	
			description.setId(null);

			
			descriptions.add(description);
			
			//add other description
			description = new ProductTypeDescription();
			if(headerMap.containsKey("type_fr")) {
				description.setId(null);
				description.setLanguage("fr");
				description.setName(record.get("type_fr"));
				description.setDescription(record.get("type_fr"));	
			} else {
				description.setId(null);
				description.setLanguage("fr");
				description.setName(record.get("type_en"));
				description.setDescription(record.get("type_en"));	
			}
			
			descriptions.add(description);
			
			type.setDescriptions(descriptions);
			
			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(type);
			
			System.out.println(json);
			
			
  		    HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

			//post to create category web service
			@SuppressWarnings("rawtypes")
			ResponseEntity response = restTemplate.postForEntity(endPoint + MERCHANT, entity, PersistableProductType.class);
			
			i++;
			System.out.println("--------------------- [" + response.getStatusCodeValue() + "]" + i);

			

			
		}
		
		
	
     
		System.out.println("------------------------------------");
		System.out.println("ProductType import done");
		System.out.println("------------------------------------");
		
	}
	
	
	private HttpHeaders getHeader(){
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		//MediaType.APPLICATION_JSON //for application/json
		headers.setContentType(mediaType);
		//Basic Authentication
		String authorisation = ADMIN_NAME + ":" + ADMIN_PASSWORD;
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
