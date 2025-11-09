const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true
})

module.exports = {
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8081', // 替换为后端实际运行的端口
                changeOrigin: true,
                pathRewrite: {
                    '^/api': '',
                },
            },
        },
    },
};