package com.olingoDemo.olingoDemo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlAliasInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.olingoDemo.olingoDemo.entities.DemoEntities;
import com.olingoDemo.olingoDemo.repository.DemoRepository;

@Component
public class DemoEdmProvider extends CsdlAbstractEdmProvider{
	@Autowired
	DemoRepository demoRepository;
	public String path="";

	public static String NAMESPACE = "OData.Demo";

	// EDM Container
	public static String CONTAINER_NAME = "Container";
	public static FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

	// Entity Types Names
	public static String ET_PRODUCT_NAME = "Product";
	public static FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

	// Entity Set Names
	//public static  String ES_PRODUCTS_NAME = "Products";
	public static String ES_PRODUCTS_NAME=ET_PRODUCT_NAME+"s";
	public void setPath(String path) {
		this.path=path;
	}
	public void setEntityType(String fileName) {
		// TODO Auto-generated method stub
		DemoEdmProvider.ET_PRODUCT_NAME=fileName; 
	}
	public void setEntitySetsName(String entitySetName) {
		DemoEdmProvider.ES_PRODUCTS_NAME= entitySetName;
	}

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
		List<DemoEntities> demoentityList= demoRepository.findAll();
		for(DemoEntities i:demoentityList) {
			DemoEdmProvider.ET_PRODUCT_NAME=i.getType(); //entity type et product name 
			DemoEdmProvider.ES_PRODUCTS_NAME=i.getName();//This sets cars.svc/$metadata correctly 
			DemoEdmProvider.ET_PRODUCT_FQN= new FullQualifiedName(NAMESPACE,ET_PRODUCT_NAME);
			entitySets.add(getEntitySet(CONTAINER,ES_PRODUCTS_NAME));

		}

		// create EntityContainer
		CsdlEntityContainer entityContainer = new CsdlEntityContainer();
		entityContainer.setName(CONTAINER_NAME);
		entityContainer.setEntitySets(entitySets);
		//System.out.println(entitySets);

		return entityContainer;
		// TODO Auto-generated method stub
		//return super.getEntityContainer();
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
		// TODO Auto-generated method stub
		// create EntitySets
		if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
			CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();

			entityContainerInfo.setContainerName(CONTAINER);

			return entityContainerInfo;
		}

		return null; 
		//return super.getEntityContainerInfo(entityContainerName);
	}

	@Override
	public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {//MODIFIED
		// TODO Auto-generated method stub
		if(entityContainer.equals(CONTAINER)){
			CsdlEntitySet entitySet = new CsdlEntitySet();
			ES_PRODUCTS_NAME=entitySetName;
			entitySet.setName(ES_PRODUCTS_NAME);
			entitySet.setType(ET_PRODUCT_FQN);
			return entitySet;
		}
		return null;
	}

	@Override
	public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException{
		// TODO Auto-generated method stub
		if (entityTypeName.equals(ET_PRODUCT_FQN)) {
			try {
				List<DemoEntities> demoentityset=demoRepository.findByName(DemoEdmProvider.ES_PRODUCTS_NAME);
				String p1 ="";
				if(demoentityset.size()==0)
					//p1=this.path;
					throw new FileNotFoundException();
				else
					p1=(demoentityset.get(0)).getFileName();

				BufferedReader br1 = new BufferedReader(new FileReader(new File(p1)));	
				ArrayList<String> setOfLines = new ArrayList<String>();
				String line1= null;String [] arrSplit = {""};
				while((line1= br1.readLine())!=null) {
					setOfLines.add(line1);
					arrSplit = setOfLines.get(0).split(","); 
				}
				//System.out.println("Length  "+arrSplit.length);
				CsdlProperty[] c= new CsdlProperty[arrSplit.length];
				for(int k=0;k<arrSplit.length;k++) {
					c[k]=new CsdlProperty().setName(arrSplit[k].replace("\"", ""))
							.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
				}
				CsdlPropertyRef propertyRef = new CsdlPropertyRef();
				propertyRef.setName(arrSplit[0].replace("\"", ""));
				// configure EntityType
				CsdlEntityType entityType = new CsdlEntityType();
				entityType.setName(ET_PRODUCT_NAME);

				entityType.setProperties(Arrays.asList(c));

				entityType.setKey(Collections.singletonList(propertyRef));
				br1.close();
				return entityType;

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}	 
		return null;
		// return super.getEntityType(entityTypeName);
	}

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		// TODO Auto-generated method stub
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace(NAMESPACE);

		// add EntityTypes
		List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
		List<DemoEntities> demoentityList= demoRepository.findAll();
		for(DemoEntities i:demoentityList) {
			DemoEdmProvider.ET_PRODUCT_NAME=i.getType(); 
			DemoEdmProvider.ES_PRODUCTS_NAME=i.getName();
			DemoEdmProvider.ET_PRODUCT_FQN= new FullQualifiedName(DemoEdmProvider.NAMESPACE, DemoEdmProvider.ET_PRODUCT_NAME);
			entityTypes.add(getEntityType(ET_PRODUCT_FQN));
		}

		schema.setEntityTypes(entityTypes);
		// add EntityContainer
		schema.setEntityContainer(getEntityContainer());

		// finally
		List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
		schemas.add(schema);
		return schemas;

	}


}
