# YOLOv10 对象检测 API 文档

本 API 提供了一个用于使用 YOLOv10 模型进行图像对象检测的端点。API 接受图像文件和可选参数以控制检测过程，并返回检测结果以及处理后的图像 URL。

## 端点：对象检测

### **POST /detect**

对上传的图像使用 YOLOv10 模型进行对象检测。

#### **请求**

- **方法**：POST
- **URL**：`/detect`
- **Content-Type**：`multipart/form-data`

##### **查询参数**

| 参数名           | 类型   | 默认值 | 描述                                                   | 是否必填 |
| ---------------- | ------ | ------ | ------------------------------------------------------ | -------- |
| `image_size`     | 整数   | 640    | 输入图像调整的大小（例如：320、416、512、640、1280）。 | 可选     |
| `conf_threshold` | 浮点数 | 0.25   | 检测对象的置信度阈值（范围：0.1 到 0.9）。             | 可选     |

##### **Body 参数**

| 参数名 | 类型         | 描述                                 | 是否必填 |
| ------ | ------------ | ------------------------------------ | -------- |
| `file` | 文件（图像） | 要处理的图像文件（例如：JPG、PNG）。 | 是       |

##### **请求示例**

```bash
curl -X POST "http://120.55.192.74:8000/detect?image_size=640&conf_threshold=0.25" \
     -F "file=@/path/to/image.jpg"
```

#### **响应**

- **Content-Type**：`application/json`
- **状态码**：
  - `200 OK`：检测成功完成。
  - `400 Bad Request`：参数无效或缺少文件。
  - `500 Internal Server Error`：服务器端处理出错。

##### **响应 Body**

| 字段名                    | 类型   | 描述                                             |
| ------------------------- | ------ | ------------------------------------------------ |
| `status`                  | 字符串 | 请求状态（`success` 或 `error`）。               |
| `message`                 | 字符串 | 错误信息（当 `status` 为 `error` 时）。          |
| `detections`              | 数组   | 检测到的对象列表（若无对象检测到则为空）。       |
| `detections[].class`      | 字符串 | 检测对象的类别名称。                             |
| `detections[].confidence` | 浮点数 | 检测的置信度得分（0 到 1）。                     |
| `detections[].bbox`       | 数组   | 边界框坐标 `[x_min, y_min, x_max, y_max]`。      |
| `result_image_url`        | 字符串 | 带有检测标注的处理后图像的 URL（相对于服务器）。 |

##### **成功响应示例**

```json
{
  "status": "success",
  "detections": [
    {
      "class": "person",
      "confidence": 0.92,
      "bbox": [150, 200, 300, 400]
    },
    {
      "class": "car",
      "confidence": 0.85,
      "bbox": [500, 300, 700, 450]
    }
  ],
  "result_image_url": "/results/detected_image_123.jpg"
}
```

##### **错误响应示例**

```json
{
  "status": "error",
  "message": "未提供图像文件",
  "detections": [],
  "result_image_url": ""
}
```

#### **注意事项**

- API 要求图像文件通过 `multipart/form-data` 请求上传。
- `result_image_url` 是服务器的相对路径。访问图像时，需在前面加上服务器基础 URL（例如：`http://<您的服务器>:8000/results/detected_image_123.jpg`）。
- 确保服务器已配置为在指定的 `result_image_url` 路径提供处理后的图像。
- `image_size` 参数会影响检测速度和准确性。较大的值（例如 1280）可能提高准确性，但会增加处理时间。
- `conf_threshold` 会过滤掉置信度低于指定值的检测结果。
