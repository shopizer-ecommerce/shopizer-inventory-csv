package com.shopizer.inventory.csv.in.manufacturer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;

public class ManufacturerImport {


	private String FILE_NAME =   "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventory-xls/shopizer-inventory-csv/src/main/resources/collection-loader.csv";
	private String endPoint = "http://localhost:8080/api/v1/private/manufacturer?store=";
	
	private static final String ADMIN_NAME = "admin@shopizer.com";
	private static final String ADMIN_PASSWORD = "password";
	
	private static final String MERCHANT = "DEFAULT";
	
	public static void main(String[] args) {
		
		ManufacturerImport manufacturerImport = new ManufacturerImport();
		try {
			manufacturerImport.process();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

		HttpHeaders httpHeader = getHeader();
		
		Map<String, Integer> headerMap = parser.getHeaderMap();

		int i = 0;
		
		for(CSVRecord record : parser){

			if(StringUtils.isBlank(record.get("code"))) {
				continue;
			}


			
			//core properties
			PersistableManufacturer manufacturer = new PersistableManufacturer();
			manufacturer.setCode(record.get("code"));
			manufacturer.setId(null);

			List<ManufacturerDescription> descriptions = new ArrayList<ManufacturerDescription>();
			
			//add english description
			ManufacturerDescription description = new ManufacturerDescription();
			description.setLanguage("en");
			description.setName(record.get("name_en"));
			description.setDescription(description.getName());	

			descriptions.add(description);
			
			//add other description
			description = new ManufacturerDescription();
			if(headerMap.containsKey("name_fr")) {
				description.setId(null);
				description.setLanguage("fr");
				description.setName(record.get("name_fr"));
				description.setDescription(record.get("name_fr"));	
			} else {
				description.setId(null);
				description.setLanguage("fr");
				description.setName(record.get("name_en"));
				description.setDescription(record.get("name_en"));	
			}
			
			descriptions.add(description);
			manufacturer.setDescriptions(descriptions);

			
			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(manufacturer);
			
			System.out.println(json);
			
			
			HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

			//post to create category web service
			//ResponseEntity response = restTemplate.postForEntity("http://beta.exotikamobilia.ca/sm-shop/services/private/DEFAULT/manufacturer", entity, PersistableManufacturer.class);
			//ResponseEntity response = restTemplate.postForEntity("http://www.exotikmobilier.com/services/private/DEFAULT/manufacturer", entity, PersistableManufacturer.class);
			ResponseEntity response = restTemplate.postForEntity(endPoint + MERCHANT, entity, PersistableManufacturer.class);
			i++;

			System.out.println("--------------------- " + i);
		}
		
		
	
     
		System.out.println("------------------------------------");
		System.out.println("Manufacturer import done");
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

}
