package org.data.extractor.entity;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WebpageRepository extends JpaRepository<Webpage, Long> {


}