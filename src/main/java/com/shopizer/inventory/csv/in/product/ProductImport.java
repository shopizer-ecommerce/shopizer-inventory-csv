package com.shopizer.inventory.csv.in.product;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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

import com.salesmanager.web.entity.catalog.category.Category;
import com.salesmanager.web.entity.catalog.manufacturer.Manufacturer;
import com.salesmanager.web.entity.catalog.product.PersistableImage;
import com.salesmanager.web.entity.catalog.product.PersistableProduct;
import com.salesmanager.web.entity.catalog.product.PersistableProductPrice;
import com.salesmanager.web.entity.catalog.product.ProductDescription;

public class ProductImport {
	
	private Map<String,Integer> skuMap = new HashMap<String,Integer>();
	
	//private String IMAGE_BASE_DIR = "/Users/carlsamson/Documents/csti/mobilia-exotika/29-11-2015-resized-600W/JPEG/";
	private String IMAGE_BASE_DIR = "/Users/carlsamson/Documents/csti/mobilia-exotika/pictures-600-resize/";
	
									
	private String FILE_NAME = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/products_20.csv";
	
	private int iterationCount = 1;

	public static void main(String[] args) {
		
		ProductImport productsImport = new ProductImport();
		try {
			productsImport.importProducts();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void importProducts() throws Exception {
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');
		
		

		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.ISO_8859_1));
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);
		
		HttpHeaders httpHeader = getHeader();

		int i = 0;
		int count = 0;
		for(CSVRecord record : parser){

			String code = record.get("sku");

			if(StringUtils.isBlank(code)) {
				System.out.println("Skipping code " + i);
				i++;
				continue;
			} else {
				//generateSku();
			}
			
			String barcode = record.get("barcode");

			if(StringUtils.isBlank(barcode)) {
				System.out.println("Skipping barcode " + i);
				i++;
				continue;
			} else {
				//generateSku();
			}
			
			String imp = record.get("import");
			
			if(StringUtils.isBlank(imp) || "no".equals(imp)) {
				System.out.println("Skipping import " + i);
				i++;
				continue;
			} else {
				//generateSku();
			}
			

			code = uniqueSku(code);
			
			String categoryCode = record.get("category");
			String price = record.get("price");
			
			String orderString = record.get("position");
			if(StringUtils.isBlank(orderString)) {
				orderString = String.valueOf(i);
			}
			
			int order = Integer.parseInt(orderString);
			
			if(StringUtils.isBlank(categoryCode)) {
				System.out.println("No category, skipping " + code);
				continue;
			}
			
			if(StringUtils.isBlank(price)) {
				System.out.println("No price, skipping " + code);
				continue;
			} else {
				//int randomPrice = this.randPrice(150, 300);//when there is no price
			}
			
			String stringQuantity = record.get("quantity");
			if(StringUtils.isBlank(stringQuantity)) {
				stringQuantity = "1";
			}
			int quantity = Integer.parseInt(stringQuantity);
			
			BigDecimal productPrice = new BigDecimal(price);
			
			Category category = new Category();
			category.setCode(categoryCode);
			List<Category> categories = new ArrayList<Category>();
			categories.add(category);
			
			
/*			System.out.println(code);
			System.out.println(record.get("name_en"));
			System.out.println(record.get("name_fr"));
			System.out.println(record.get("title_en"));
			System.out.println(record.get("title_fr"));
			System.out.println(record.get("friendlyUrl_en"));
			System.out.println(record.get("friendlyUrl_fr"));
			System.out.println(record.get("position"));
			System.out.println(record.get("visible"));
			System.out.println(record.get("parent"));*/
			
			String manufacturer = record.get("collection");
			Manufacturer collection = new Manufacturer();
			collection.setCode(manufacturer);
			
			//core properties
			PersistableProduct product = new PersistableProduct();
			product.setSku(code);
			product.setRefSku(barcode);
			product.setManufacturer(collection);
			product.setCategories(categories);
			
			product.setSortOrder(order);//set the order or iterator as sort order
			product.setAvailable(true);//force availability
			product.setProductVirtual(false);//force tangible good
			product.setQuantityOrderMinimum(1);//force to 1 minimum when ordering
			product.setProductShipeable(true);//all items are shipeable
			
			/** images **/
			String image = record.get("image_file");
			if(!StringUtils.isBlank(image)) {
				
				StringBuilder imageName = new StringBuilder();
				imageName.append(IMAGE_BASE_DIR).append(image);
				
				File imgPath = new File(imageName.toString());
				
				PersistableImage persistableImage = new PersistableImage();
				
				
				persistableImage.setBytes(this.extractBytes(imgPath));
				persistableImage.setImageName(imgPath.getName());

				List<PersistableImage> images = new ArrayList<PersistableImage>();
				images.add(persistableImage);
				
				product.setImages(images);
				
			}

			
			String dimensions = record.get("dimensions");

			product.setProductHeight(convertDimension(record.get("height"),dimensions));
			product.setProductWidth(convertDimension(record.get("width"),dimensions));
			product.setProductLength(convertDimension(record.get("length"),dimensions));


			product.setProductWeight(convertWeight(record.get("weight")));
			

			product.setQuantity(quantity);
			product.setQuantityOrderMaximum(maximumQuantity(quantity));
			product.setQuantityOrderMinimum(minimumQuantity(quantity));

			PersistableProductPrice persistableProductPrice = new PersistableProductPrice();
			persistableProductPrice.setDefaultPrice(true);

			persistableProductPrice.setOriginalPrice(productPrice);
			
			//apply a discunted price
			String discount = record.get("deal");
			BigDecimal discountedPrice = this.createDiscountedPrice(productPrice, discount);
			if(discountedPrice != null) {
				persistableProductPrice.setDiscountedPrice(discountedPrice);
			}
			
			List<PersistableProductPrice> productPriceList = new ArrayList<PersistableProductPrice>();
			productPriceList.add(persistableProductPrice);
			
			product.setProductPrices(productPriceList);

			List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

			//add english description
			
/*			ProductDescription description = new ProductDescription();
			description.setLanguage("en");
			//description.setTitle(record.get("title_en"));
			description.setTitle(record.get("name_en"));
			description.setName(record.get("name_en"));
			description.setDescription(record.get("description_en"));
			//description.setFriendlyUrl(record.get("friendlyUrl_en"));
			description.setFriendlyUrl(minimalFriendlyUrlCreator(description.getName()));
			//description.setHighlights(record.get("highlights_en"));
			
			descriptions.add(description);*/
			
			ProductDescription description = new ProductDescription();
			
			//add french description
			description = new ProductDescription();
			description.setLanguage("fr");
			description.setTitle(record.get("name_fr") + " - Exotik Mobilier");
			description.setName(record.get("name_fr"));
			description.setDescription(record.get("description_fr"));
			description.setFriendlyUrl(minimalFriendlyUrlCreator(description.getName()));
			//description.setHighlights(record.get("highlights_fr"));
			
			descriptions.add(description);

			product.setDescriptions(descriptions);
			
			
			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(product);
			
			//System.out.println(json);
			
			
			HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

			//post to create category web service
			ResponseEntity response = restTemplate.postForEntity("http://www.exotikmobilier.com/services/private/DEFAULT/product", entity, PersistableProduct.class);
			PersistableProduct prod = (PersistableProduct) response.getBody();
			

			
			//System.out.println("--------------------- " + i);
			i++;//rows
			count++;
			
			//if(i==30) {
			//	break;
			//}
		}
		
		
		System.out.println("------------------------------------");
		System.out.println("Product import done " + count);
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
	
	
	/**
	 * Can be used to generate a sku on the fly
	 * @return
	 */
	private String generateSku() {
		String sku = UUID.randomUUID().toString();
		sku = sku.replace("-", "");
		return sku;
	}
	
	/**
	 * Makes sure that the sku is unique
	 * if not, add suffix of a numeric incrementable value
	 * @return
	 */
	private String uniqueSku(String sku) {
		sku = sku.replace("-", "");
		sku = sku.replace(" ", "");
		if(skuMap.containsKey(sku)) {
			int count = skuMap.get(sku);
			count = count ++;
			sku = count+ sku;
		} else {
			skuMap.put(sku, iterationCount);
		}
		
		return sku;
	}
	
	private BigDecimal convertDimension(String value, String dimensions) {
		
		//in our case we convert from cm to inches 1 * 0.393701
		BigDecimal decimalValue = new BigDecimal(value);
		
		if(StringUtils.isBlank(dimensions) || "CM".equals(dimensions)) {
			decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		}

		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		return scaled;

	}
	
	private BigDecimal convertWeight(String value) {
		
		//in our case we already have the good weight
		BigDecimal decimalValue = new BigDecimal(value);
		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		//decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		return scaled;

	}
	
	private BigDecimal createDiscountedPrice(BigDecimal price, String discount) {
		
		if(StringUtils.isNotBlank(discount)) {
			if("50".equals(discount)) {
				price = price.multiply(new BigDecimal(0.50));
			}
			if("30".equals(discount)) {
				price = price.multiply(new BigDecimal(0.70));
			}

			
			return roundAmount(price);
		}
		
		
		return null;
		
	}
	
	private BigDecimal roundAmount(BigDecimal amount) {
		BigDecimal scaled = amount.setScale(0, RoundingMode.HALF_UP);
		return scaled;
	}
	
	public byte[] extractBytes (File imgPath) throws Exception {
		
		FileInputStream fis = null;
		BufferedInputStream inputStream = null;
		try {
			fis = new FileInputStream(imgPath);
			inputStream = new BufferedInputStream(fis);
	        byte[] fileBytes = new byte[(int) imgPath.length()];
	        inputStream.read(fileBytes);
	        return fileBytes;
        } finally {
        	if(inputStream != null) {
        		inputStream.close();
        	}
        	if(fis != null) {
        		fis.close();
        	}
        }
		

	}
	
	public int randPrice(int min, int max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
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
	
	public int maximumQuantity(int quantity) {
		if(quantity==1) {
			return quantity;
		}
		
		if(quantity>0) {
			return quantity ++;
		}
		
		return quantity;
	}
	
	public int minimumQuantity(int quantity) {

		return Integer.parseInt("1");
	}

}
