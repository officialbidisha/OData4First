package com.olingoDemo.olingoDemo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.olingoDemo.olingoDemo.entities.DemoEntities;
import com.olingoDemo.olingoDemo.repository.DemoRepository;

//@Component
@Service
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

	private String path;
	private OData odata;
	private int countData;
	private ServiceMetadata serviceMetadata;
	@Autowired
	private DemoRepository demoRepository;
	private String entityUrl;

	public void setPath(String path) { // for just upload for first time used only once

		this.path=path;
		System.out.println("Path check "+path); 

	}
	public void setentityName(String entityUrl, boolean shouldbeSaved) { // Demo1 or Demo2
		this.entityUrl=entityUrl;
		if(shouldbeSaved) { 
			System.out.println("The saved name with which we search is : "+this.entityUrl);
			//DemoEntities ent= new DemoEntities((this.entityUrl),("http://localhost:8080/cars.svc/"+entityUrl),this.path,DemoEdmProvider.ET_PRODUCT_NAME);
			DemoEntities ent= new DemoEntities(this.entityUrl,("http://myawsbucketbs.s3-website.us-east-2.amazonaws.com/cars.svc/"+entityUrl),this.path,DemoEdmProvider.ET_PRODUCT_NAME);

			demoRepository.save(ent); 
		}
	}
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata=odata;
		this.serviceMetadata=serviceMetadata;	
	}

	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
		ContentType responseFormat) throws ODataApplicationException, ODataLibraryException, SerializerException {
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		List<UriParameter> keyParams = uriResourceEntitySet.getKeyPredicates();
		System.out.println("Keyparams is "+keyParams);
		
		EntityCollection entitySet = null;
		try {
			entitySet = getData(edmEntitySet);//entitySet=entityCollection
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<Entity> entityList = entitySet.getEntities();
		EntityCollection returnEntityCollection = new EntityCollection();

		OrderByOption orderByOption = uriInfo.getOrderByOption();
		if (orderByOption != null) {
			List<OrderByItem> orderItemList = orderByOption.getOrders();
			final OrderByItem orderByItem = orderItemList.get(0); // we support only one
			Expression expression = orderByItem.getExpression();
			if(expression instanceof Member){
				UriInfoResource resourcePath = ((Member)expression).getResourcePath();
				UriResource uriResource = resourcePath.getUriResourceParts().get(0);
				if (uriResource instanceof UriResourcePrimitiveProperty) {
					EdmProperty edmProperty = ((UriResourcePrimitiveProperty)uriResource).getProperty();
					final String sortPropertyName = edmProperty.getName();

					// do the sorting for the list of entities  
					Collections.sort(entityList, new Comparator<Entity>() {

						// delegate the sorting to native sorter of Integer and String
						public int compare(Entity entity1, Entity entity2) {
							int compareResult = 0;

							if(sortPropertyName.equals("ID")){
								Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
								Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();

								compareResult = integer1.compareTo(integer2);
							}else{
								String propertyValue1 = (String) entity1.getProperty(sortPropertyName).getValue();
								String propertyValue2 = (String) entity2.getProperty(sortPropertyName).getValue();

								compareResult = propertyValue1.compareTo(propertyValue2);
							}

							// if 'desc' is specified in the URI, change the order
							if(orderByItem.isDescending()){
								return - compareResult; // just reverse order
							}

							return compareResult;
						}
					});
				}
			}
		}

		CountOption countOption = uriInfo.getCountOption();
		// System.out.println("COUNT "+uriInfo.getCountOption().getName());
		if (countOption != null) {
			boolean isCount = countOption.getValue();
			if(isCount){
				returnEntityCollection.setCount(entityList.size());
			}
		}

		// handle $top
		TopOption topOption = uriInfo.getTopOption();
		//System.out.println("TOP "+uriInfo.getTopOption().getName());
		if (topOption != null) {
			int topNumber = topOption.getValue();
			if (topNumber >= 0) {
				if(topNumber <= entityList.size()) {
					entityList = entityList.subList(0, topNumber);
				}  // else the client has requested more entities than available => return what we have
			} else {
				throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}

		SelectOption selectOption = uriInfo.getSelectOption();

		for(Entity entity : entityList){
			returnEntityCollection.getEntities().add(entity);
		}

		ODataSerializer serializer = odata.createSerializer(responseFormat);

		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType,
				null, selectOption);
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();

		Entity requestedEntity = findEntity(edmEntityType, entitySet, keyParams);
		//System.out.println("Requested entity is: "+ requestedEntity);

		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl)
				.count(countOption).select(selectOption).build();
		SerializerResult serializedContent = serializer.entityCollection(serviceMetadata, edmEntityType, returnEntityCollection, opts);
		response.setContent(serializedContent.getContent());


		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}
	public static Entity findEntity(EdmEntityType edmEntityType,
			EntityCollection rt_entitySet, List<UriParameter> keyParams)
					throws ODataApplicationException {

		List<Entity> entityList = rt_entitySet.getEntities();

		// loop over all entities in order to find that one that matches all keys in request
		// an example could be e.g. contacts(ContactID=1, CompanyID=1)
		for(Entity rt_entity : entityList){
			boolean foundEntity = entityMatchesAllKeys(edmEntityType, rt_entity, keyParams);
			if(foundEntity){
				return rt_entity;
			}
		}

		return null;
	}
	public static boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity,  List<UriParameter> keyParams)
			throws ODataApplicationException {

		// loop over all keys
		for (final UriParameter key : keyParams) {
			// key
			String keyName = key.getName();
			String keyText = key.getText();

			// Edm: we need this info for the comparison below
			EdmProperty edmKeyProperty = (EdmProperty )edmEntityType.getProperty(keyName);
			Boolean isNullable = edmKeyProperty.isNullable();
			Integer maxLength = edmKeyProperty.getMaxLength();
			Integer precision = edmKeyProperty.getPrecision();
			Boolean isUnicode = edmKeyProperty.isUnicode();
			Integer scale = edmKeyProperty.getScale();
			// get the EdmType in order to compare
			EdmType edmType = edmKeyProperty.getType();
			// Key properties must be instance of primitive type
			EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType)edmType;

			// Runtime data: the value of the current entity
			Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

			// now need to compare the valueObject with the keyText String
			// this is done using the type.valueToString //
			String valueAsString = null;
			try {
				valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
						precision, scale, isUnicode);
			} catch (EdmPrimitiveTypeException e) {
				throw new ODataApplicationException("Failed to retrieve String value",
						HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),Locale.ENGLISH, e);
			}

			if (valueAsString == null){
				return false;
			}

			boolean matches = valueAsString.equals(keyText);
			if(!matches){
				// if any of the key properties is not found in the entity, we don't need to search further
				return false;
			}
		}

		return true;
	}


	private EntityCollection getData(EdmEntitySet edmEntitySet) throws FileNotFoundException {

		System.out.println("Is entering in gerData()");
		EntityCollection dataToSend = new EntityCollection();

		if (DemoEdmProvider.ES_PRODUCTS_NAME.equals(edmEntitySet.getName())) {
			List<Entity> productList = dataToSend.getEntities();

			// New Code Starts here
			try
			{
				this.countData=0;
				List<DemoEntities> demoentityset=demoRepository.findByName(DemoEdmProvider.ET_PRODUCT_NAME); // extracting the record using url. entityurl is demo1demo2
				String p1 ="";
				if(demoentityset.size()==0)
					//p1=this.path;
					throw new FileNotFoundException();
				else
					p1=demoentityset.get(0).getFileName();
				BufferedReader reader = new BufferedReader(new FileReader(p1));
				List<String> linesz = new ArrayList<String>();
				String line1 = null;
				while ((line1 = reader.readLine()) != null) {
					linesz.add(line1);
				}
				reader.close();
				System.out.println(linesz.get(0));
				String []sp = linesz.get(0).split(",");
				
				BufferedReader br = new BufferedReader(new FileReader(new File(p1)));
				br.readLine();
				String line = "";
				int lineNumber = 0;
				while ((line = br.readLine()) != null) {
					String[] arr = line.split(",");
					
					int l= arr.length;
					if(l==sp.length) {
						for(int i=0;i<l;)
						{
							Entity e1 = new Entity();
							for(int y=0;y<sp.length;y++) {/*originally sp.length*/
								e1.addProperty(new Property(null, sp[y].replace("\"", ""), ValueType.PRIMITIVE, arr[i++].replace("\"", "")));
							}
							e1.setId(createId(this.entityUrl, this.countData++));
							productList.add(e1);
						}
						lineNumber++;
					}
					else {	  
						int i;

						for(i=0;i<l;) //0 to 3
						{

							Entity e1 = new Entity();
							int y=0;
							for( y=0;y<l;y++) {
								e1.addProperty(new Property(null, sp[y].replace("\"", ""), ValueType.PRIMITIVE, arr[i++].replace("\"", "")));
							}
							e1.setId(createId(this.entityUrl, this.countData++));
							productList.add(e1);

						} 
						lineNumber++;
						
					}

				}
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}
			//New Code Ends Here


		}

		return dataToSend;
	}
	private URI createId(String entitySetName, Object id) {
		try {
			return new URI(entitySetName + "(" + String.valueOf(id) + ")");
		} catch (URISyntaxException e) {
			throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
		}
	}


}
