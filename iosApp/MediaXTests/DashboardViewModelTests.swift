@preconcurrency import SharedLogic
import XCTest
@testable import MediaX

@MainActor
final class DashboardViewModelTests: XCTestCase {
    func testLoadPublishesContent() async {
        let expected = DashboardSnapshot(
            summary: DashboardSummary(totalItems: 4, publishedItems: 3, totalViews: 250),
            recentMedia: []
        )
        let viewModel = DashboardViewModel(loader: { expected })

        await viewModel.load()

        guard case let .content(snapshot) = viewModel.state else {
            return XCTFail("Expected dashboard content")
        }
        XCTAssertEqual(snapshot.summary.totalItems, 4)
    }

    func testLoadPublishesFriendlyFailure() async {
        let viewModel = DashboardViewModel(loader: { throw TestError.unavailable })

        await viewModel.load()

        guard case let .failure(message) = viewModel.state else {
            return XCTFail("Expected failure state")
        }
        XCTAssertEqual(message, "We couldn't load your dashboard.")
    }

    private enum TestError: Error {
        case unavailable
    }
}

