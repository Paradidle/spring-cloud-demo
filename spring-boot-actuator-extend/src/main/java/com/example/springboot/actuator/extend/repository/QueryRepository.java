package com.example.springboot.actuator.extend.repository;

import com.example.springboot.actuator.extend.domain.QueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2021 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2021/8/1
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2021/8/1；
 */
public interface QueryRepository extends JpaRepository<QueryEntity,String> {
}
