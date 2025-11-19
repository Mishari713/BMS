package com.inspire.tasks.roles;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoleTypes name;

    public Role(RoleTypes name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RoleTypes getName() {
        return name;
    }

    public void setName(RoleTypes name) {
        this.name = name;
    }
}