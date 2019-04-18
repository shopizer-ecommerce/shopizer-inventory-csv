package com.shopizer.inventory.csv.in.manufacturer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

import com.salesmanager.web.entity.catalog.manufacturer.ManufacturerDescription;
import com.salesmanager.web.entity.catalog.manufacturer.PersistableManufacturer;

public class ManufacturerImport {

	private String FILE_NAME = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/manufacturer-loader.csv";
	
	
	public static void main(String[] args) {
		
		ManufacturerImport manufacturerImport = new ManufacturerImport();
		try {
			manufacturerImport.importManufacturer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void importManufacturer() throws Exception {
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');

		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.ISO_8859_1));
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);

		HttpHeaders httpHeader = getHeader();

		int i = 0;
		
		for(CSVRecord record : parser){

			if(StringUtils.isBlank(record.get("code"))) {
				continue;
			}
			
			
/*			System.out.println(record.get("code"));
			System.out.println(record.get("name_en"));
			System.out.println(record.get("name_fr"));
			System.out.println(record.get("title_en"));
			System.out.println(record.get("title_fr"));
			System.out.println(record.get("friendly_url_en"));
			System.out.println(record.get("friendly_url_fr"));*/

			
			//core properties
			PersistableManufacturer manufacturer = new PersistableManufacturer();
			manufacturer.setCode(record.get("code"));

			List<ManufacturerDescription> descriptions = new ArrayList<ManufacturerDescription>();
			
			//add english description
			ManufacturerDescription description = new ManufacturerDescription();
			description.setLanguage("en");
			description.setTitle(record.get("title_en"));
			description.setName(record.get("name_en"));
			description.setDescription(description.getName());	
			description.setFriendlyUrl(record.get("friendly_url_en"));
			
			descriptions.add(description);
			
			//add french description
			description = new ManufacturerDescription();
			description.setLanguage("fr");
			description.setTitle(record.get("title_fr"));
			description.setName(record.get("name_fr"));
			description.setDescription(description.getName());
			description.setFriendlyUrl(record.get("friendly_url_fr"));
			
			descriptions.add(description);
			manufacturer.setDescriptions(descriptions);
			
			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(manufacturer);
			
			System.out.println(json);
			
			
			HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

			//post to create category web service
			//ResponseEntity response = restTemplate.postForEntity("http://beta.exotikamobilia.ca/sm-shop/services/private/DEFAULT/manufacturer", entity, PersistableManufacturer.class);
			ResponseEntity response = restTemplate.postForEntity("http://www.exotikmobilier.com/services/private/DEFAULT/manufacturer", entity, PersistableManufacturer.class);
			PersistableManufacturer manuf = (PersistableManufacturer) response.getBody();
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
		String authorisation = "admin" + ":" + "Montreal2016!";
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}

}
