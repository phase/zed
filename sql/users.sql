CREATE TABLE `users` (
    `id` int(32) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `name` varchar(32) NOT NULL,
    `password` varchar(255) NOT NULL,
    `points` int(32) NOT NULL DEFAULT '0'
);