package com.cars.material.bean;

/**
 * 授权状态信息Bean类
 * 用于处理授权详情接口返回的数据
 */
public class AuthorizationInfo {
    
    /**
     * 授权状态数据
     */
    private AuthorizationData data;
    
    /**
     * 授权状态数据内部类
     */
    public static class AuthorizationData {
        /**
         * 授权状态：
         * "1" - 已通过授权
         * "0" - 已关闭授权
         * "2" - 正在授权中
         * "3" - 已驳回授权
         */
        private String status;
        
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        
        /**
         * 判断是否有状态值
         * @return true表示有状态值，false表示需要申请
         */
        public boolean hasStatus() {
            return status != null && !status.isEmpty();
        }
        
        /**
         * 获取状态描述文本
         * @return 状态描述
         */
        public String getStatusText() {
            if (status == null || status.isEmpty()) {
                return "申请授权";
            }

            switch (status) {
                case "1":
                    return "已通过授权";
                case "0":
                    return "已关闭授权";
                case "2":
                    return "正在授权中";
                case "3":
                    return "已驳回授权";
                default:
                    return "申请授权";
            }
        }
        
        /**
         * 获取状态对应的图标资源名称
         * @return 图标资源名称
         */
        public String getStatusIcon() {
            if (status == null || status.isEmpty()) {
                return "sqm_sqsq";
            }

            switch (status) {
                case "1":
                    return "sqm_ytgsq";
                case "0":
                    return "sqm_ygbsq";
                case "2":
                    return "sqm_sqshz";
                case "3":
                    return "sqm_ybhsq";
                default:
                    return "sqm_sqsq";
            }
        }
        
        /**
         * 获取状态对应的背景颜色
         * @return 颜色值
         */
        public String getStatusBackgroundColor() {
            if (status == null || status.isEmpty()) {
                return "#FFF4D5";
            }

            switch (status) {
                case "1":
                    return "#0DAA01";
                case "0":
                    return "#FF171A";
                case "2":
                    return "#363636";
                case "3":
                    return "#E6E6FC";
                default:
                    return "#FFF4D5";
            }
        }
        
        /**
         * 获取状态对应的文本颜色
         * @return 文本颜色值
         */
        public String getStatusTextColor() {
            if (status == null || status.isEmpty()) {
                return "#333333";
            }

            switch (status) {
                case "1":
                case "0":
                case "2":
                    return "#ffffff";
                case "3":
                    return "#333333";
                default:
                    return "#333333";
            }
        }
    }
    
    public AuthorizationData getData() {
        return data;
    }
    
    public void setData(AuthorizationData data) {
        this.data = data;
    }
    
    /**
     * 判断是否需要申请授权（data为null或status为null）
     * @return true表示需要申请，false表示已有状态
     */
    public boolean needApply() {
        return data == null || !data.hasStatus();
    }
    
    /**
     * 获取授权状态值，用于H5页面参数传递
     * @return 状态值，如果需要申请则返回null
     */
    public String getStatusForH5() {
        if (needApply()) {
            return null;
        }
        return data.getStatus();
    }
}
