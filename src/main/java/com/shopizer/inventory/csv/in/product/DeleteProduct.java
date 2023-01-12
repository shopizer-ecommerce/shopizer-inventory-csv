package com.shopizer.inventory.csv.in.product;

import java.nio.charset.Charset;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;


/**
 * Delete products from a list of ids
 * @author carlsamson
 *
 */
public class DeleteProduct {
	
	
	public static void main(String[] args) {
		
		DeleteProduct deleteImport = new DeleteProduct();
		try {
			deleteImport.deleteProducts();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private HttpHeaders httpHeader;
	
	private String endPoint = "https://demo-api.shopizer.com/api/v1/private/product";

	private static final String MERCHANT = "DEFAULT";
	private boolean DRY_RUN = false;
	
	private final int START_COUNT = 93;
	private final int MAX_COUNT = 121;
	
	
	public void deleteProducts() throws Exception {
		
		
		
		RestTemplate restTemplate = new RestTemplate();

		
		HttpHeaders httpHeader = getHeader();

		int count = 0;
		for(int i = START_COUNT; i <= MAX_COUNT + START_COUNT; i = i+ 2){

			
			
			System.out.println("Line " + count + " ******************** " + i);
		
			if(!DRY_RUN) {
				ResponseEntity<Void> response = restTemplate.exchange(endPoint + "/" + i, HttpMethod.DELETE, new HttpEntity<>(Void.class, httpHeader), Void.class); 
				int statusCode = response.getStatusCodeValue();
				System.out.println("Status code " + statusCode);
			}
			

			//i++;//rows
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
