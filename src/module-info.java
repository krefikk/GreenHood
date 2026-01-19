/**
 * 
 */
/**
 * 
 */
module DisposalShare {
	requires java.sql; // for SQL operations
	requires java.desktop;
	requires jbcrypt; // hashing library to hash and salt passwords
	requires com.zaxxer.hikari; // pooling library
    requires org.slf4j; // logging library which pooling library uses
    requires java.mail; // sending mails to users
}