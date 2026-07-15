package com.anverraglobal.insurance.auth.repository;

import com.anverraglobal.insurance.auth.entity.Role;
import com.anverraglobal.insurance.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
