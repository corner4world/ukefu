package com.ukefu.webim.service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ukefu.webim.web.model.QualityAppeal;

public abstract interface QualityAppealRepository extends JpaRepository<QualityAppeal,String>{
	
	public abstract Page<QualityAppeal> findAll(Specification<QualityAppeal> spec, Pageable page) ;
	
	public abstract List<QualityAppeal> findByOrgi(String orgi) ;

}