package com.olingoDemo.olingoDemo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.edmx.EdmxReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


import com.olingoDemo.olingoDemo.entities.DemoEntities;
import com.olingoDemo.olingoDemo.repository.DemoRepository;
import com.olingoDemo.olingoDemo.service.DemoEdmProvider;
import com.olingoDemo.olingoDemo.service.DemoEntityCollectionProcessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;

@RestController
@CrossOrigin(origins = "http://myawsbucketbs.s3-website.us-east-2.amazonaws.com") 
//@CrossOrigin(origins="http://localhost:4200")
@RequestMapping(path=DemoController.URI)
@Api(value="Metadata & Service Document", description="Operations related to the metadata and service document")
public class DemoController {
	List<String> files = new ArrayList<String>();
	public String finalLocation="";
	public final Path rootLocation = Paths.get("/home/ubuntu/csvfiles");
	//public final Path rootLocation = Paths.get("C:\\AllFiles");
	@Autowired
	private DemoRepository demoRepository;

	//private static final Logger LOG = LoggerFactory.getLogger(DemoServlet.class);
	public static final String URI = "/cars.svc";

	/** The edm provider. */
	@Autowired
	private DemoEdmProvider demoedmProvider;

	/** The enity collection processor. */
	@Autowired
	private DemoEntityCollectionProcessor demoenityCollectionProcessor;


	/**
	 * Process.
	 *
	 * @param request the req
	 * @param response the Http response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	@ApiOperation(value = "View a list of metadata and service document",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	
	@RequestMapping(value = "*") // apart from the time of upload, this executes always
	
	public void process(HttpServletRequest request, HttpServletResponse response) {
		//CSV file is loaded, Converting the .csv to odata

		OData odata = OData.newInstance();
		ServiceMetadata edm = odata.createServiceMetadata(demoedmProvider,
				new ArrayList<EdmxReference>());
		String entityName=request.getRequestURI().replace((URI+"/"),"");
		if(!entityName.equals("") && !entityName.equals("$metadata")) {
			System.out.println("Enterhere when sending with Demo1s or News");
			demoedmProvider.setEntityType(entityName);
			System.out.println("Entity Name is :"+entityName);		
		}
		demoenityCollectionProcessor.setentityName(entityName,false);// don't save into db
		ODataHttpHandler handler = odata.createHandler(edm);
		handler.register(demoenityCollectionProcessor);
		handler.process(new HttpServletRequestWrapper(request) {
			@Override
			public String getServletPath() {

				return DemoController.URI;
			}
		}, response);
	}
	@ApiOperation(value="View all the records in the Database")
	@GetMapping("/urls")
	public  List<DemoEntities> getListFiles() {

		return this.demoRepository.findAll();
	}
	@ApiOperation(value="Used to save a file")
	@PostMapping("/savefile")
	public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("fileName") String fileName) {
		String message;
		try {
			try {
				UUID random= UUID.randomUUID();
				System.out.println("the name of file "+fileName);
				System.out.println(file.getOriginalFilename());
				Files.copy(file.getInputStream(), this.rootLocation.resolve(random+file.getOriginalFilename()));
				this.finalLocation=rootLocation+File.separator+random+file.getOriginalFilename();

			} catch (Exception e) {	
				throw new RuntimeException("FAIL!");
			}
			files.add(file.getOriginalFilename());


			demoedmProvider.setEntityType(fileName);
			demoedmProvider.setEntitySetsName(fileName+"s");
			demoenityCollectionProcessor.setPath(this.finalLocation);
			demoenityCollectionProcessor.setentityName((fileName+"s"),true);
			demoedmProvider.setPath(this.finalLocation);
			message = "Successfully uploaded!";
			return ResponseEntity.status(HttpStatus.OK).body(message);
		} catch (Exception e) {
			message = "Failed to upload!";
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(message);
		}
	}

}
