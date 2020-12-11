package com.olingoDemo.olingoDemo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.olingoDemo.olingoDemo.entities.DemoEntities;

@Repository
public interface DemoRepository extends JpaRepository<DemoEntities, Long> {
	public List<DemoEntities> findByName(String name);
}
