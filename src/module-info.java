module FoundationsF26 {
	requires javafx.controls;
	requires java.sql;
	requires org.junit.jupiter.api;

	opens applicationMain to javafx.graphics, javafx.fxml;
	opens hw3Tests to org.junit.platform.commons;
	opens curatorLockTests to org.junit.platform.commons;
}
