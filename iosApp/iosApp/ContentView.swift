import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "tram.fill")
                .imageScale(.large)
                .foregroundColor(.accentColor)
            Text("Econex Planer iOS")
                .font(.title)
                .bold()
            Text("Kotlin Multiplatform Ready")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}
