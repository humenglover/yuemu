import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from 'axios';

interface WeatherData {
  weather: string;
  temperature: string;
  city: string;
  province: string;
  timestamp: number;
  adcode: string;  // 添加adcode字段以便调试
}

interface WeatherResponse {
  status: string;
  info: string;
  infocode: string;
  lives: Array<{
    province: string;
    city: string;
    adcode: string;
    weather: string;
    temperature: string;
    winddirection: string;
    windpower: string;
    humidity: string;
    reporttime: string;
    temperature_float: string;
    humidity_float: string;
  }>;
}

interface IpLocationResponse {
  status: string;
  info: string;
  infocode: string;
  province: string;
  city: string;
  adcode: string;
  rectangle: string;
}

export const useWeatherStore = defineStore('weather', () => {
  const weatherData = ref<WeatherData | null>(null);
  const CACHE_KEY = 'weather_cache';
  const CACHE_DURATION = 1 * 60 * 1000; // 临时改为1分钟缓存，方便测试
  const AMAP_KEY = '';

  // 从本地存储加载天气数据
  const loadFromCache = (): WeatherData | null => {
    try {
      const cached = localStorage.getItem(CACHE_KEY);
      if (!cached) {
        console.log('没有找到缓存数据');
        return null;
      }

      const data: WeatherData = JSON.parse(cached);
      const now = Date.now();

      // 检查数据是否过期
      if (now - data.timestamp > CACHE_DURATION) {
        console.log('缓存数据已过期，当前缓存:', data);
        localStorage.removeItem(CACHE_KEY);
        return null;
      }

      console.log('使用缓存数据，城市代码:', data.adcode);
      return data;
    } catch (error) {
      console.error('加载缓存天气数据失败:', error);
      localStorage.removeItem(CACHE_KEY);
      return null;
    }
  };

  // 保存数据到本地存储
  const saveToCache = (data: WeatherData) => {
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify({
        ...data,
        timestamp: Date.now()
      }));
      console.log('已保存到缓存，数据:', data);
    } catch (error) {
      console.error('保存天气数据到缓存失败:', error);
    }
  };

  // 获取IP定位信息
  const getLocationByIp = async (): Promise<string> => {
    try {
      console.log('开始获取IP定位...');
      const ipResponse = await axios.get<IpLocationResponse>(
        `https://restapi.amap.com/v3/ip?key=${AMAP_KEY}`,
        {
          // 添加时间戳防止缓存
          params: {
            t: Date.now()
          }
        }
      );

      console.log('IP定位原始响应:', ipResponse.data);

      if (ipResponse.data.status === '1') {
        if (!ipResponse.data.adcode) {
          console.error('IP定位返回的adcode为空');
          throw new Error('IP定位返回的adcode为空');
        }

        console.log('IP定位成功 - 省份:', ipResponse.data.province);
        console.log('IP定位成功 - 城市:', ipResponse.data.city);
        console.log('IP定位成功 - adcode:', ipResponse.data.adcode);

        return ipResponse.data.adcode;
      }

      console.error('IP定位API返回错误:', ipResponse.data.info);
      throw new Error(`IP定位失败: ${ipResponse.data.info}`);
    } catch (error) {
      console.error('IP定位请求失败:', error);
      throw error;
    }
  };

  // 获取天气数据
  const fetchWeather = async () => {
    try {
      console.log('开始获取天气数据...');

      // 先检查缓存
      const cachedData = loadFromCache();
      if (cachedData) {
        console.log('使用缓存的天气数据:', cachedData);
        weatherData.value = cachedData;
        return;
      }

      // 先获取IP定位
      const adcode = await getLocationByIp();
      console.log('准备使用adcode获取天气:', adcode);

      // 获取天气信息
      const weatherResponse = await axios.get<WeatherResponse>(
        `https://restapi.amap.com/v3/weather/weatherInfo`,
        {
          params: {
            key: AMAP_KEY,
            city: adcode,
            extensions: 'base',
            t: Date.now() // 添加时间戳防止缓存
          }
        }
      );

      console.log('天气API原始响应:', weatherResponse.data);

      if (weatherResponse.data.status === '1' && weatherResponse.data.lives?.length > 0) {
        const live = weatherResponse.data.lives[0];
        console.log('解析的天气数据:', live);

        const newWeatherData: WeatherData = {
          weather: live.weather,
          temperature: live.temperature,
          city: live.city,
          province: live.province,
          adcode: live.adcode, // 保存adcode以便调试
          timestamp: Date.now()
        };

        console.log('保存到store的天气数据:', newWeatherData);
        weatherData.value = newWeatherData;
        saveToCache(newWeatherData);
      } else {
        console.error('天气API返回数据格式错误:', weatherResponse.data);
        throw new Error('天气数据获取失败');
      }
    } catch (error) {
      console.error('获取天气信息失败:', error);
      // 如果请求失败，尝试使用缓存数据（即使已过期）
      const cachedData = loadFromCache();
      if (cachedData) {
        console.log('使用过期的缓存数据:', cachedData);
        weatherData.value = cachedData;
      }
    }
  };

  return {
    weatherData,
    fetchWeather
  };
});
