package com.shopizer.inventory.csv.in.product;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

public class DeleteProductImport {
	
	
	public static void main(String[] args) {
		
		DeleteProductImport deleteImport = new DeleteProductImport();
		try {
			deleteImport.deleteProducts();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String langs[] = {"en"};
	
	private HttpHeaders httpHeader;
	
	private String endPoint = "http://localhost:8080/api/v1/private/product";
	
	private String FILE_NAME = "/Users/carlsamson/Documents/dev/mydidas.csv";
	
	private static final String MERCHANT = "DEFAULT";
	private boolean DRY_RUN = false;
	
	private final int MAX_COUNT = 25000;
	
	
	public void deleteProducts() throws Exception {
		
		
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');

		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.UTF_8));
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);

		
		HttpHeaders httpHeader = getHeader();

		int i = 0;
		int count = 0;
		for(CSVRecord record : parser){

			String code = record.get("product_id");

			if(StringUtils.isBlank(code)) {
				throw new Exception("No skus");
			} 
			
			if(!DRY_RUN) {

				//ResponseEntity<String> response
				//  = restTemplate.getForEntity(endPoint + "/" + code, String.class, httpHeader);
				
				ResponseEntity<Void> response = restTemplate.exchange(endPoint + "/" + code, HttpMethod.DELETE, new HttpEntity<>(Void.class, httpHeader), Void.class); 
				
				///Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
				
				
				//HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
				//ResponseEntity response = restTemplate.get
				//		.postForEntity(endPoint + MERCHANT, entity, Void.class);
				int statusCode = response.getStatusCodeValue();
				System.out.println("Status code " + statusCode);
			}

			
			System.out.println("Line " + i + " ******************** " + code);



			i++;//rows
			count++;

		}
		
		
		System.out.println("------------------------------------");
		System.out.println("Product import done " + count + " Dry Run [" + DRY_RUN + "]");
		System.out.println("------------------------------------");
		
	}

	
    
	private HttpHeaders getHeader(){
		if (DRY_RUN) return null;
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



}
