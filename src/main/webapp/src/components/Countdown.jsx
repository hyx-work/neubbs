import { Component } from 'react'
import PropTypes from 'prop-types'
import { connect } from 'dva'

class Countdown extends Component {
  constructor(props) {
    super(props)

    const { start, duration } = props
    const now = Date.now()
    const count = start ? duration - Math.floor((now - start) / 1000) : 0

    this.state = {
      start,
      count,
    }

    this.timer = 0
    this.startTimer = this.startTimer.bind(this)
    this.countdown = this.countdown.bind(this)
  }

  componentWillMount() {
    if (this.state.start !== 0) {
      this.startTimer()
    } else if (this.props.onMount) {
      this.props.onMount()
    }
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.start !== nextProps.start) {
      this.setState({
        start: nextProps.start,
        count: nextProps.duration,
      })
      this.props.dispatch({
        type: 'app/setCountdown',
        payload: {
          type: nextProps.type,
          start: nextProps.start,
        },
      })
      this.startTimer()
    }
  }

  componentWillUnmount() {
    clearInterval(this.timer)
  }

  startTimer() {
    const { delay } = this.props

    if (this.timer === 0) {
      this.timer = setInterval(this.countdown, delay * 1000)
    }
  }

  countdown() {
    const { duration } = this.props
    const { start } = this.state
    const now = Date.now()
    const count = duration - Math.floor((now - start) / 1000)

    if (count <= 0) {
      clearInterval(this.timer)
      this.props.dispatch({
        type: 'app/setCountdown',
        payload: {
          type: this.props.type,
          start: 0,
        },
      })
      this.timer = 0
    }
    this.setState({ count })
  }

  render() {
    const { count } = this.state
    return this.props.render(count, this.startTimer)
  }
}

Countdown.propTypes = {
  start: PropTypes.number.isRequired,
  type: PropTypes.string.isRequired,
  delay: PropTypes.number,
  duration: PropTypes.number.isRequired,
  onMount: PropTypes.func.isRequired,
  render: PropTypes.func.isRequired,
}

Countdown.defaultProps = {
  delay: 1,
  autoStart: false,
}

export default connect()(Countdown)

