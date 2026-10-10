package com.edupulse.repository;

import com.edupulse.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    java.util.List<FollowUp> findByInterventionId(Long interventionId);
}

