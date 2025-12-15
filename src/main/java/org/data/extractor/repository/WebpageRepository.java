package org.data.extractor.repository;


import org.data.extractor.entity.Webpage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface WebpageRepository extends JpaRepository<Webpage, Long> {


}