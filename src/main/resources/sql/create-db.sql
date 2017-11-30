DROP TABLE user IF EXISTS;
CREATE TABLE user (
  user_id INTEGER PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL
);


DROP TABLE post IF EXISTS;
CREATE TABLE post (
  post_id INTEGER PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
  author_id INTEGER NOT NULL FOREIGN KEY REFERENCES user(user_id),
  wall_id INTEGER NOT NULL FOREIGN KEY REFERENCES user(user_id),
  text VARCHAR(160) NOT NULL,
  pub_date TIMESTAMP
);

DROP TABLE likes IF EXISTS;
CREATE TABLE likes (
	like_id INTEGER PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	post_id INTEGER NOT NULL FOREIGN KEY REFERENCES post(post_id),
	user_id INTEGER NOT NULL FOREIGN KEY REFERENCES user(user_id)
);