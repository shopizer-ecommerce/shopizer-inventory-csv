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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.product.PersistableImage;
import com.salesmanager.shop.model.catalog.product.PersistableProduct;
import com.salesmanager.shop.model.catalog.product.PersistableProductPrice;
import com.salesmanager.shop.model.catalog.product.ProductDescription;
import com.salesmanager.shop.model.catalog.product.ProductSpecification;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductAttribute;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValue;



public class ProductImport {
	
	private Map<String,Integer> skuMap = new HashMap<String,Integer>();
	
	/**
	 * Fields used in xls file with Product mapping
	 * 
	 * barcode
	 * sku
	 * image
	 * image_file
	 * name_<lang>
	 * description_<lang>
	 * quantity
	 * collection
	 * category
	 * height
	 * width
	 * length
	 * weight
	 * price
	 * deal -> % discounted
	 * pre-order
	 * dimensions -> CM | IN (centimeters or inches)
	 * position -> placement in listing page (ordering)
	 * import -> yes | no
	 * 
	 * NEED JAVA 1.8
	 */
	
	/**
	 * Supported languages
	 * CSV template must contain name_<language> and description<language>
	 */
	private String langs[] = {"en","fr"};
	
	private HttpHeaders httpHeader;
	
	private String endPoint = "http://localhost:8080/api/v1/private/product?store=";
	
	private static final String MERCHANT = "DEFAULT";
	private boolean DRY_RUN = false;
	
	private final int MAX_COUNT = 100;
	
	
	
	/**
	 * Where to find images
	 */
	private String IMAGE_BASE_DIR = "/Users/carlsamson/Documents/csti/projects-proposals/bam/BAM-excel/";
	
	/**
	 * where to find csv							
	 */
	private String FILE_NAME = "/Users/carlsamson/Documents/csti/projects-proposals/rufina/xls/rufina-product-import-2.csv";
	//private String FILE_NAME = "/Users/carlsamson/Documents/csti/shopizer/Revamp-2.5/Shopizer-demo/products-import.csv";
	//private String FILE_NAME = "/Users/carlsamson/Documents/csti/projects-proposals/bam/BAM-excel/BAM-import_list_02.csv";
	//private String FILE_NAME = "/Users/carlsamson/Documents/dev/workspaces/shopizer-tools/tools/src/main/resources/BAM-import.csv";
	
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
		                      new FileInputStream(FILE_NAME), StandardCharsets.UTF_8));
		
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
			} 
			
			
			String barcode = "";
			if(record.isSet("barcode")) {
				barcode = record.get("barcode");
				if(!StringUtils.isBlank(barcode)) {
					barcode = this.alternativeIdentifier(record);
					if(StringUtils.isBlank(barcode)) {
						System.out.println("Skipping barcode " + i);
						i++;
						continue;
					}
				}
			}
			
			
			String imp = record.get("import");//import this row ? yes|no
			System.out.println("Import status [" + imp + "]");
			
			if(StringUtils.isBlank(imp) || "no".equals(imp)) {
				System.out.println("Skipping import " + i);
				i++;
				continue;
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
			price = price.replaceAll(",", ".").trim();
			
			BigDecimal productPrice = new BigDecimal(price);
			
			Category category = new Category();
			category.setCode(categoryCode);
			List<Category> categories = new ArrayList<Category>();
			categories.add(category);

			
			/** specification **/
			String manufacturer = record.get("collection");//brand - manufacturer ...
			ProductSpecification specs = new ProductSpecification();
			specs.setManufacturer(manufacturer);
			
			//sizes are required when loading a product
			String dimensions = record.get("dimension");
			
			String W = convertDimension(record.get("package_width"),dimensions).toString();
			String L = convertDimension(record.get("package_length"),dimensions).toString();
			String H = convertDimension(record.get("package_length"),dimensions).toString();
			
			specs.setHeight(convertDimension(record.get("package_height"),dimensions));
			specs.setWidth(convertDimension(record.get("package_width"),dimensions));
			specs.setLength(convertDimension(record.get("package_length"),dimensions));
			specs.setWeight(convertWeight(record.get("package_weight")));

			//core properties
			PersistableProduct product = new PersistableProduct();
			product.setSku(code);
			product.setRefSku(barcode);
			product.setProductSpecifications(specs);
			product.setCategories(categories);
			
			product.setSortOrder(order);//set the order or iterator as sort order
			product.setAvailable(true);//force availability
			product.setProductVirtual(false);//force tangible good
			product.setQuantityOrderMinimum(1);//force to 1 minimum when ordering
			product.setProductShipeable(true);//all items are shipeable
			
			if(record.isSet("type")) {
				product.setType(record.get("type"));
			}
			
			if(record.isSet("dimensions")) {
				String dimensionsOptions = record.get("dimensions");
				//should be : seperated
				
				if(!StringUtils.isBlank(dimensionsOptions)) {
					PersistableProductOption opt = new PersistableProductOption();
					opt.setCode("size");
					List<String> dims = getTokensWithCollection(dimensionsOptions,":");
					List<PersistableProductAttribute> attributes = new ArrayList<PersistableProductAttribute>();
					dims.stream().forEach(s -> {
						PersistableProductAttribute attr = new PersistableProductAttribute();

						attr.setOption(opt);
						PersistableProductOptionValue optValue = new PersistableProductOptionValue();
						optValue.setCode(s);
						
						attr.setOptionValue(optValue);
						
						attributes.add(attr);
					});
					product.setAttributes(attributes);

				}
			}
			
			/** images **/
			if(record.isSet("image_name")) {
				String image = record.get("image_name");
				if(!StringUtils.isBlank(image)) {
					
					StringBuilder imageName = new StringBuilder();
					imageName.append(IMAGE_BASE_DIR).append(image.trim()).append(".jpg");//TODO remove
					
					File imgPath = new File(imageName.toString());
					
					byte[] bytes = this.extractBytes(imgPath);
					if(bytes!=null) {
						PersistableImage persistableImage = new PersistableImage();
	
						persistableImage.setBytes(bytes);
						persistableImage.setName(imgPath.getName());
			
						List<PersistableImage> images = new ArrayList<PersistableImage>();
						images.add(persistableImage);
							
						product.setImages(images);
					}
				}
			}


		
			product.setQuantity(quantity);
			product.setQuantityOrderMaximum(maximumQuantity(quantity));
			product.setQuantityOrderMinimum(minimumQuantity(quantity));

			PersistableProductPrice persistableProductPrice = new PersistableProductPrice();
			persistableProductPrice.setDefaultPrice(true);

			persistableProductPrice.setOriginalPrice(productPrice);
			
			//apply a discunted price
			if(record.isSet("deal")) {
				String discount = record.get("deal");
				BigDecimal discountedPrice = this.createDiscountedPrice(productPrice, discount);
				if(discountedPrice != null) {
					persistableProductPrice.setDiscountedPrice(discountedPrice);
				}
			}
			
			List<PersistableProductPrice> productPriceList = new ArrayList<PersistableProductPrice>();
			productPriceList.add(persistableProductPrice);
			
			product.setProductPrices(productPriceList);

			List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

						
			for(int langLenth=0;langLenth<langs.length;langLenth++) {
				
				ProductDescription description = new ProductDescription();
				String lang = langs[langLenth];
				if(!record.isSet("pre") || !StringUtils.isBlank(record.get("pre-order"))) {
					//something specific must be written ?
				}

				
				description = new ProductDescription();
				description.setLanguage(lang);
				description.setTitle(cleanup(record.get("name_" + lang)));
				description.setName(cleanup(record.get("name_" + lang)));
				description.setDescription(cleanup(record.get("description_" + lang)));
				if(StringUtils.isBlank(description.getName())) {
					description.setName(description.getDescription());
				}
				description.setFriendlyUrl(minimalFriendlyUrlCreator(description.getName()));
				descriptions.add(description);
				
			}

			product.setDescriptions(descriptions);

			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(product);
			
			System.out.println("Line " + i + " ********************");
			System.out.println(json);

			
			

			//post to create category v0 API web service

			if(!DRY_RUN) {
				HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
				ResponseEntity response = restTemplate.postForEntity(endPoint + MERCHANT, entity, PersistableProduct.class);
				PersistableProduct prod = (PersistableProduct) response.getBody();
			}
			
			System.out.println("Created product in row " + i + " Dry Run [" + DRY_RUN + "]");

			i++;//rows
			count++;
			
			if(i==MAX_COUNT) {
				System.out.println("Reached MAX_COUNT [" + MAX_COUNT + "]");
				break;
			}
		}
		
		
		System.out.println("------------------------------------");
		System.out.println("Product import done " + count + " Dry Run [" + DRY_RUN + "]");
		System.out.println("------------------------------------");
		
	}
	
	private String cleanup(String field) {
		//cleanup unwelcome other charset characters
		return field.replaceAll("�", "");
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
    
    private ProductOptionValue optValue(String code) {
    	ProductOptionValue optValue = new ProductOptionValue();
    	optValue.setCode(code);
    	return optValue;
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
		
		return sku.trim();
	}
	
	private BigDecimal convertDimension(String value, String dimensions) {
		
		//in our case we convert from cm to inches 1 * 0.393701
		if(StringUtils.isBlank(value)) {
			value = "0";
		}
		value=value.replaceAll(",",".").trim();
		//System.out.println("Dimension " + value);
		BigDecimal decimalValue = new BigDecimal(value);
		
		if(StringUtils.isBlank(dimensions) || "CM".equals(dimensions)) {
			decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		}

		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		return scaled;

	}
	
	private BigDecimal convertWeight(String value) {
		
		//in our case we already have the good weight
		if(StringUtils.isBlank(value)) {
			value = "0";
		}
		value=value.replaceAll(",",".").trim();
		BigDecimal decimalValue = new BigDecimal(value);
		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		//decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		return scaled;

	}
	
	private BigDecimal createDiscountedPrice(BigDecimal price, String discount) {
		
		if(StringUtils.isNotBlank(discount) && !discount.equals("0")) {
			double percent = 1 - Double.parseDouble(discount) / 100d;
			price = price.multiply(new BigDecimal(percent));
			return roundAmount(price);
		}

		return null;
		
	}
	
	private BigDecimal roundAmount(BigDecimal amount) {
		BigDecimal scaled = amount.setScale(0, RoundingMode.HALF_UP);
		return scaled;
	}
	
	public byte[] extractBytes (File imgPath) throws Exception {
		
		if(!imgPath.exists()) {
			System.out.println("--------------------------------------");
			System.out.println("IMAGE NOT FOUND " + imgPath.getName());
			System.out.println("IMAGE PATH " + imgPath.getAbsolutePath());
			System.out.println("--------------------------------------");
			return null;
		}
		
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
	
	private List<String> getTokensWithCollection(String str, String token) {
	    return Collections.list(new StringTokenizer(str,token)).stream()
	      .map(t -> (String) t)
	      .collect(Collectors.toList());
	}
	
	public int minimumQuantity(int quantity) {

		return Integer.parseInt("1");
	}
	
	private String alternativeIdentifier(CSVRecord record) {
		return record.get("sku");
	}

}
