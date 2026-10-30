import Foundation
@preconcurrency import SharedLogic

@MainActor
final class DashboardViewModel: ObservableObject {
    enum State {
        case loading
        case content(DashboardSnapshot)
        case failure(String)
    }

    @Published private(set) var state: State = .loading

    private let loader: () async throws -> DashboardSnapshot

    convenience init(repository: MediaRepository = InMemoryMediaRepository(simulatedDelayMillis: 180)) {
        self.init(loader: {
            try await repository.loadDashboard()
        })
    }

    init(loader: @escaping () async throws -> DashboardSnapshot) {
        self.loader = loader
    }

    func load() async {
        state = .loading
        do {
            state = .content(try await loader())
        } catch {
            state = .failure("We couldn't load your dashboard.")
        }
    }
}

