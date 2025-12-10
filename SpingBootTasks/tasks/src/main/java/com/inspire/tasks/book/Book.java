package com.inspire.tasks.book;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inspire.tasks.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "books", uniqueConstraints = {
        @UniqueConstraint(columnNames = "author_name"),
        @UniqueConstraint(columnNames = "title")
})
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "title")
    String title;

    @Column(name = "author_name")
    String authorName;

    @Column(name = "description")
    String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User userId;

    public Book(String title, String authorName, String description, User userId) {
        this.title = title;
        this.authorName = authorName;
        this.description = description;
        this.userId = userId;
    }
}
