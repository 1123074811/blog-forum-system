// 调试翻译功能的脚本
// 在浏览器控制台中运行

async function testTranslation() {
    console.log('开始测试翻译功能...');
    
    // 测试1: 基本字段翻译
    try {
        const response = await fetch('/api/translate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                text: 'employee_name',
                context: 'database_field'
            })
        });
        
        console.log('测试1 - 响应状态:', response.status);
        
        if (response.ok) {
            const result = await response.json();
            console.log('测试1 - 翻译结果:', result);
        } else {
            console.error('测试1 - 请求失败:', response.status, response.statusText);
            const errorText = await response.text();
            console.error('测试1 - 错误详情:', errorText);
        }
    } catch (error) {
        console.error('测试1 - 网络错误:', error);
    }
    
    // 测试2: 表名翻译
    try {
        const response = await fetch('/api/translate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                text: 'employees',
                context: 'table'
            })
        });
        
        console.log('测试2 - 响应状态:', response.status);
        
        if (response.ok) {
            const result = await response.json();
            console.log('测试2 - 翻译结果:', result);
        } else {
            console.error('测试2 - 请求失败:', response.status, response.statusText);
        }
    } catch (error) {
        console.error('测试2 - 网络错误:', error);
    }
}

// 运行测试
testTranslation();