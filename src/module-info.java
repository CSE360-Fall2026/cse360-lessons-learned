module FoundationsF26 {
	requires javafx.controls;
	requires java.sql;
	requires org.junit.jupiter.api;
	requires java.net.http;

	opens applicationMain to javafx.graphics, javafx.fxml;
	opens hw3Tests to org.junit.platform.commons;
	opens curatorLockTests to org.junit.platform.commons;
	opens curatorHomeTests to org.junit.platform.commons;
	opens curatorRefinementTests to org.junit.platform.commons;
	opens lessonTests to org.junit.platform.commons;
	opens curatorTests to org.junit.platform.commons;
	opens viewerTests to org.junit.platform.commons;
	opens entityClasses to org.junit.platform.commons;
	opens inputValidation to org.junit.platform.commons;
	opens database to org.junit.platform.commons;
	opens curatorSummaryTests to org.junit.platform.commons;
}
