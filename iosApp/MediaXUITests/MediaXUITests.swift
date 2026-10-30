import XCTest

final class MediaXUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testDashboardLaunches() {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.staticTexts["dashboard.title"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Recent media"].exists)
    }
}

