package org.data.extractor.repository;


import org.data.extractor.entity.Webpage;
import org.data.extractor.pojo.ParliamentaryPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WebpageRepository extends JpaRepository<Webpage, Long> {


//    List<Webpage> findByParliamentaryPeriod(String period);

    List<Webpage> findByParliamentaryPeriod(String period);

    @Query(nativeQuery = true, value = "SELECT CAST(congreso.webpage.inserted_time AS DATE) AS date_only FROM congreso.webpage;")
    List<String> findAllFormatted();



}