package com.example.survey.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
@Data
@NoArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "user_id")
    private String userId;

    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne
    @JoinColumn
    private Question question;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option selectedOption;

    private String response;
}
