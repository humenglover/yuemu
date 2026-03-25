from typing import List


class RAGConstants:
    """RAG服务核心常量（移除官网相关词汇版）"""
    # 基础配置
    DEFAULT_TOP_K = 8
    DEFAULT_TEMPERATURE = 0.7
    DEFAULT_MAX_TOKENS = 2048
    SUPPORTED_FILE_EXTENSIONS = ['.pdf', '.txt', '.docx', '.md']

    # 日志事件类型
    LOG_EVENT_REQUEST_START = "REQUEST_START"
    LOG_EVENT_REQUEST_COMPLETE = "REQUEST_COMPLETE"
    LOG_EVENT_REQUEST_ERROR = "REQUEST_ERROR"
    LOG_EVENT_STREAM_REQUEST_START = "STREAM_REQUEST_START"
    LOG_EVENT_STREAM_REQUEST_COMPLETE = "STREAM_REQUEST_COMPLETE"
    LOG_EVENT_STREAM_REQUEST_ERROR = "STREAM_REQUEST_ERROR"
    LOG_EVENT_UPLOAD_START = "UPLOAD_START"
    LOG_EVENT_UPLOAD_SUCCESS = "UPLOAD_SUCCESS"
    LOG_EVENT_UPLOAD_ERROR = "UPLOAD_ERROR"
    LOG_EVENT_UPLOAD_DUPLICATE = "UPLOAD_DUPLICATE"
    LOG_EVENT_UPLOAD_NO_CONTENT = "UPLOAD_NO_CONTENT"
    LOG_EVENT_UPLOAD_SPLIT_FAILED = "UPLOAD_SPLIT_FAILED"
    LOG_EVENT_AI_ANSWER_START = "AI_ANSWER_START"
    LOG_EVENT_AI_ANSWER_COMPLETE = "AI_ANSWER_COMPLETE"
    LOG_EVENT_AI_ANSWER_ERROR = "AI_ANSWER_ERROR"
    LOG_EVENT_STREAM_AI_ANSWER_START = "STREAM_AI_ANSWER_START"
    LOG_EVENT_STREAM_AI_ANSWER_ERROR = "STREAM_AI_ANSWER_ERROR"

    # 会话相关
    SESSION_ID_PREFIX = "session_"

    # Agent触发关键词
    REAL_TIME_INDICATORS = [
        # 时间类（保留核心实时词）
        "今天", "现在", "实时", "最新", "此刻", "当前", "当下", "近期",
        "最近", "今日", "本日", "本月", "本周", "本年度", "今年", "此刻",
        "此时此刻", "最新消息", "最新资讯", "最新动态", "最新进展", "最新情况",
        # 信息获取类（保留核心，无官网相关词）
        "新闻", "天气", "时间", "日期", "搜索", "网络", "上网", "查一下",
        "查一查", "问问", "查找", "检索", "查询", "求证", "核实", "确认",
        "了解", "知晓", "掌握", "获取", "得知", "获悉", "打听", "探听",
        # 实时数据类（移除官网/网站/网页，保留核心实时词）
        "实时信息", "实时数据", "实时更新", "实时播报", "实时行情", "实时走势",
        "在线", "联网", "联机", "网查", "线上",
        # 金融财经类（保留）
        "股价", "股票", "行情", "汇率", "价格", "利率", "金价", "银价",
        "油价", "期货", "指数", "大盘", "涨停", "跌停", "成交量", "成交额",
        # 生活服务类（保留）
        "天气预报", "空气质量", "交通状况", "航班信息", "列车时刻", "快递查询",
        "话费余额", "水电费", "燃气费", "物业费", "实时路况", "拥堵情况"
    ]

    URL_PATTERNS = [
        # 基础URL标识
        'http', 'https', 'www.', '.com', '.cn', '.org', '.net', '.gov', '.edu',
        # 常见域名后缀
        '.io', '.me', '.cc', '.tv', '.biz', '.info', '.name', '.top', '.xyz',
        # URL特征字符
        '/', '?', '=', '&', '#', '@', ':', '//', 'ftp', 'sftp', 'mailto'
    ]

    REASONING_KEYWORDS = [
        # 分析类
        "分析", "剖析", "解析", "解读", "研判", "评估", "考量", "审视",
        "剖析", "梳理", "归纳", "演绎", "推导", "论证", "阐释", "说明",
        # 对比类
        "对比", "比较", "对照", "类比", "比照", "差异", "区别", "异同",
        "优劣", "好坏", "高低", "强弱", "大小", "多少", "长短", "利弊",
        # 总结类
        "总结", "概括", "归纳", "提炼", "整理", "汇总", "概述", "简述",
        "小结", "总结归纳", "概括总结", "核心要点", "主要内容", "关键信息",
        # 推理判断类
        "推理", "推断", "推论", "推导", "演绎", "归纳", "逻辑", "思辨",
        "判断", "判定", "断定", "判别", "甄别", "分辨", "识别", "区分",
        # 预测决策类
        "预测", "预估", "预计", "推测", "猜想", "猜测", "预判", "展望",
        "趋势", "走向", "前景", "未来", "发展", "演变", "变化", "走向"
    ]

    FORCE_AGENT_KEYWORDS = [
        # 强制要求类
        "强制返回", "必须回答", "不管怎样都要", "无视业务范围",
        "即使不在业务范围也要", "一定要回答", "务必回答", "必须给出",
        "无论如何", "不管如何", "不管怎样", "无论怎样", "横竖都要",
        # 强硬要求类
        "必须告知", "必须说明", "必须解释", "必须提供", "必须给出",
        "不许拒绝", "不得拒绝", "禁止拒绝", "不能拒绝", "无法拒绝",
        # 特殊要求类
        "无视规则", "忽略规则", "跳过规则", "绕过规则", "不按规则",
        "打破常规", "特殊处理", "例外处理", "特事特办", "破例回答"
    ]

    # 业务范围判断提示词
    BUSINESS_SCOPE_PROMPT = """
    业务范围：本地知识库查询、实时信息获取（天气/股市/新闻）、通用知识问答与推理。
    非业务范围：违法违规、色情暴力、政治敏感、无意义闲聊、专业领域深度未收录问题。
    仅返回"是"或"否"判断用户问题是否属于业务范围，无需任何额外解释。
    """


class HttpStatusCodes:
    """HTTP状态码常量"""
    OK = 200
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    INTERNAL_SERVER_ERROR = 500