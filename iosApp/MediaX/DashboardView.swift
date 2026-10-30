@preconcurrency import SharedLogic
import SwiftUI

struct DashboardView: View {
    @StateObject private var viewModel: DashboardViewModel

    init(viewModel: DashboardViewModel = DashboardViewModel()) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        NavigationStack {
            Group {
                switch viewModel.state {
                case .loading:
                    ProgressView("Loading dashboard")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityIdentifier("dashboard.loading")
                case let .content(snapshot):
                    DashboardContent(snapshot: snapshot)
                case let .failure(message):
                    ErrorView(message: message) {
                        Task { await viewModel.load() }
                    }
                }
            }
            .background(Color(uiColor: .systemGroupedBackground))
            .navigationBarHidden(true)
        }
        .task {
            await viewModel.load()
        }
    }
}

private struct DashboardContent: View {
    let snapshot: DashboardSnapshot

    private let columns = [GridItem(.adaptive(minimum: 180), spacing: 12)]

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("MediaX")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.indigo)
                    Text("Media dashboard")
                        .font(.largeTitle.bold())
                        .accessibilityIdentifier("dashboard.title")
                    Text("Your content performance at a glance.")
                        .font(.body)
                        .foregroundStyle(.secondary)
                }

                LazyVGrid(columns: columns, spacing: 12) {
                    MetricCard(label: "Total media", value: "\(snapshot.summary.totalItems)")
                    MetricCard(label: "Published", value: "\(snapshot.summary.publishedItems)")
                    MetricCard(label: "Total views", value: snapshot.summary.totalViews.compactFormatted)
                }

                Text("Recent media")
                    .font(.title2.bold())
                    .padding(.top, 4)

                if snapshot.recentMedia.isEmpty {
                    EmptyMediaView()
                } else {
                    ForEach(snapshot.recentMedia, id: \.id) { item in
                        MediaRow(item: item)
                    }
                }
            }
            .padding(20)
        }
    }
}

private struct MetricCard: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.semibold))
            Text(value)
                .font(.title.bold())
        }
        .foregroundStyle(Color.indigo)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.indigo.opacity(0.12), in: RoundedRectangle(cornerRadius: 20))
        .accessibilityElement(children: .combine)
    }
}

private struct MediaRow: View {
    let item: MediaItem

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: item.kind.symbolName)
                .font(.title3)
                .foregroundStyle(.indigo)
                .frame(width: 48, height: 48)
                .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14))

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.headline)
                    .lineLimit(1)
                Text("\(item.kind.displayName) · \(item.publishedAtEpochSeconds.formattedDate)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 4) {
                Text(item.status.displayName)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(item.status.name == "PUBLISHED" ? Color.indigo : Color.secondary)
                if item.viewCount > 0 {
                    Text("\(item.viewCount.compactFormatted) views")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 18))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(item.title), \(item.status.displayName)")
    }
}

private struct EmptyMediaView: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "rectangle.stack")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("No recent media")
                .font(.headline)
            Text("New content will appear here.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }
}

private struct ErrorView: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.indigo)
            Text("Something went wrong")
                .font(.title2.bold())
            Text(message)
                .foregroundStyle(.secondary)
            Button("Retry", action: retry)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(32)
        .accessibilityIdentifier("dashboard.error")
    }
}

private extension MediaKind {
    var symbolName: String {
        switch name {
        case "VIDEO": "film"
        case "AUDIO": "waveform"
        default: "photo"
        }
    }

    var displayName: String {
        name.lowercased().capitalized
    }
}

private extension MediaStatus {
    var displayName: String {
        name.lowercased().capitalized
    }
}

private extension Int64 {
    var compactFormatted: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 1

        if self >= 1_000_000 {
            return "\(formatter.string(from: NSNumber(value: Double(self) / 1_000_000)) ?? "0")M"
        }
        if self >= 1_000 {
            return "\(formatter.string(from: NSNumber(value: Double(self) / 1_000)) ?? "0")K"
        }
        return formatter.string(from: NSNumber(value: self)) ?? "\(self)"
    }

    var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(self)))
    }
}

#Preview {
    DashboardView()
}

