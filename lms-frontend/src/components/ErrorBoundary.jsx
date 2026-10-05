import { Component } from 'react'

export default class ErrorBoundary extends Component {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() {
    if (!this.state.failed) return this.props.children
    return (
      <div className="page">
        <h1>Something went wrong</h1>
        <p className="muted">Reload the page to try again.</p>
        <button className="btn" onClick={() => window.location.reload()}>Reload</button>
      </div>
    )
  }
}
