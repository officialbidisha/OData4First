package com.olingoDemo.olingoDemo.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.swagger.annotations.ApiModelProperty;

@Entity
@Table(name="CSVDetails")
public class DemoEntities {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@ApiModelProperty(notes="The auto generated id for each record")
	private long id;
	@ApiModelProperty(notes="The entity type name")
	private String type;
	@ApiModelProperty(notes="The entity set name")
	private String name;
	@ApiModelProperty(notes="The entity url")
	private String url;
	@ApiModelProperty(notes="The actual file path")
	private String fileName;

	@Column(name="fileName", nullable=false)
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public DemoEntities(){}
	public DemoEntities( String name, String url,String fileName,String type) {
		super();
		//this.id = id;
		this.name = name;
		this.url = url;
		this.fileName=fileName;
		this.type=type;
	}



	//	public long getId() {
	//		return id;
	//	}
	//	public void setId(int id) {
	//		this.id = id;
	//	}

	@Column(name = "name", nullable = false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "url", nullable = false)
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	@Override
	public String toString() {
		return "DemoEntities [id=" + id + ", name=" + name + ", url=" + url + ", fileName=" + fileName + "]";
	}
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}

}
