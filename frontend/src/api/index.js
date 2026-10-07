/**
 * 接口统一出口
 * 视图层可 `import { parcelApi, authApi } from '@/api'` 使用命名空间方式调用，
 * 也可直接 `import { pageParcels } from '@/api/parcel'` 按模块引入。
 */
import * as authApi from './auth'
import * as parcelApi from './parcel'
import * as shipApi from './ship'
import * as exceptionApi from './exception'
import * as statsApi from './stats'
import * as userApi from './user'
import * as roleApi from './role'
import * as stationApi from './station'
import * as shelfApi from './shelf'

export { authApi, parcelApi, shipApi, exceptionApi, statsApi, userApi, roleApi, stationApi, shelfApi }

export default {
  auth: authApi,
  parcel: parcelApi,
  ship: shipApi,
  exception: exceptionApi,
  stats: statsApi,
  user: userApi,
  role: roleApi,
  station: stationApi,
  shelf: shelfApi
}
