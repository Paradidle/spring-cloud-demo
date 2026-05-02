import json
import sys


def main():
    input_data = json.loads(sys.stdin.read())
    
    params = input_data.get('params', {})
    
    stocks = [
        {
            "code": "600519",
            "name": "贵州茅台",
            "technicalScore": 85.5,
            "trend": "up"
        },
        {
            "code": "000858",
            "name": "五粮液",
            "technicalScore": 78.2,
            "trend": "up"
        }
    ]
    
    result = {
        "stocks": stocks,
        "skillName": "technical",
        "period": params.get('period', '7d')
    }
    
    print(json.dumps(result))

if __name__ == '__main__':
    main()
