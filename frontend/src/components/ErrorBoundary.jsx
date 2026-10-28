import React from 'react';

export default class ErrorBoundary extends React.Component {
    state = {failed: false};

    static getDerivedStateFromError() {
        return {failed: true};
    }

    render() {
        if (this.state.failed) return <main className="centered"><h1>Algo não saiu como esperado</h1>
            <button onClick={() => this.setState({failed: false})}>Tentar novamente</button>
        </main>;
        return this.props.children;
    }
}