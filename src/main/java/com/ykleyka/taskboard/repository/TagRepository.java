package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

}
