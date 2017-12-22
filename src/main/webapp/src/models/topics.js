import { routerRedux } from 'dva/router'
import { parse } from 'qs'
import _ from 'lodash'

import topics from '../services/topics'
import * as routes from '../config/routes'

function filterTopics(data) {
  return _.sortBy(_.uniqBy(data, 'topicid'), 'createtime').reverse()
}

export default {
  namespace: 'topics',

  state: {
    topicList: {
      all: [],
      categorys: {},
      users: {},
    },
    page: 0,
    pageTotal: 1,
    categorys: [],
    querying: false,
  },

  subscriptions: {
    setup({ dispatch, history }) {
      history.listen(({ pathname, search }) => {
        if (pathname === routes.ROOT) {
          const query = parse(search.substr(1))
          const { category } = query

          dispatch({ type: 'resetPage' })
          dispatch({
            type: 'pages',
            payload: {
              limit: 25,
              category,
            },
          })
          dispatch({
            type: 'query',
            payload: {
              page: 1,
              limit: 25,
              category,
            },
          })
        }
      })
    },
  },

  effects: {
    * create(action, { call }) {
      const { payload = {} } = action
      const { title, content, category } = payload

      const { data } = yield call(topics.create, {
        title, content, category,
      })

      try {
        if (data.success) {
          const { topicid } = data.model
          routerRedux.push(routes.TOPIC_DETAIL.replace(':id', topicid))
        } else {
          throw data.message
        }
      } catch (err) {
        throw err
      }
    },

    * query(action, { put, call }) {
      const { payload = {} } = action
      const {
        page, limit, category, username,
      } = payload

      yield put({ type: 'startQuery' })
      const { data } = yield call(topics.query, {
        page, limit, category, username,
      })

      try {
        if (data.success) {
          yield put({
            type: 'setTopics',
            payload: data.model,
            meta: { category, username, page },
          })
        } else {
          throw data.message
        }
      } catch (err) {
        throw err
      }
    },

    * pages(action, { put, call }) {
      const { limit, category, username } = action.payload
      const { data } = yield call(topics.pages, { limit, category, username })

      try {
        if (data.success) {
          yield put({ type: 'setPageTotal', payload: data.model })
        } else {
          throw data.message
        }
      } catch (err) {
        throw err
      }
    },

    * categorys(action, { put, call }) {
      const { data } = yield call(topics.categorys)
      try {
        if (data.success) {
          yield put({ type: 'setCategorys', payload: data.model })
        } else {
          throw data.message
        }
      } catch (err) {
        throw err
      }
    },
  },

  reducers: {
    startQuery(state) {
      return {
        ...state,
        querying: true,
      }
    },

    setTopics(state, action) {
      const { payload, meta } = action
      const { category, username, page } = meta
      const { all, categorys, users } = state.topicList

      let topicList = {
        all: filterTopics(all.concat(payload)),
      }

      // category topics
      if (category) {
        const old = categorys[category] || []
        topicList = {
          categorys: {
            ...categorys,
            [category]: filterTopics(old.concat(payload)),
          },
        }
      }

      // user topics
      if (username) {
        const old = users[username] || []
        topicList = {
          users: {
            ...users,
            [username]: filterTopics(old.concat(payload)),
          },
        }
      }

      return {
        ...state,
        querying: false,
        topicList: {
          ...state.topicList,
          ...topicList,
        },
        page,
      }
    },

    setPageTotal(state, action) {
      const { totalpages } = action.payload
      return {
        ...state,
        pageTotal: totalpages,
      }
    },

    setCategorys(state, action) {
      return {
        ...state,
        categorys: action.payload,
      }
    },

    resetPage(state) {
      return {
        ...state,
        page: 0,
      }
    },

  },
}