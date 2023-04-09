package my.lazyskulptor.commerce.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Account
 */
@Entity
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String password;

	private String email;

	private String firstName;

	private String lastName;

	private Boolean enabled;

	@CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "account_id"))
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "authority_name")
	@Enumerated(EnumType.STRING)
	private Set<Authority> authorities;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
