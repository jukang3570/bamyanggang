from flask import Flask, request, send_file, jsonify
from flask_cors import CORS
import io
from PIL import Image, ImageFilter
import torch
from diffusers import StableDiffusionPipeline
import os
import uuid  # For generating unique filenames
import re  # For sanitizing filenames
import logging

app = Flask(__name__)
CORS(app)  # Ensure CORS is properly configured

# Configure logging
logging.basicConfig(level=logging.INFO)

# Stable Diffusion 모델 로드
model_id = "CompVis/stable-diffusion-v1-4"
pipe = StableDiffusionPipeline.from_pretrained(model_id, torch_dtype=torch.float32)
pipe = pipe.to("cuda" if torch.cuda.is_available() else "cpu")

# 이미지 저장 디렉토리 설정
IMAGE_SAVE_DIR = "generated_images"
os.makedirs(IMAGE_SAVE_DIR, exist_ok=True)

def sanitize_filename(command):
    """Remove or replace characters that are not allowed in filenames."""
    sanitized = re.sub(r'[^\w\-_\. ]', '_', command)
    return sanitized

@app.route('/generate', methods=['POST'])
def generate_image():
    data = request.get_json()
    command = data.get('command')
    app.logger.info(f"Received command: {command}")

    try:
        # 명령어를 사용하여 이미지 생성
        image = pipe(command).images[0]
        app.logger.info("Image generated successfully")

        # 고유한 파일명 생성
        sanitized_command = sanitize_filename(command)
        unique_filename = f"{sanitized_command}_{uuid.uuid4()}.png"
        image_filepath = os.path.join(IMAGE_SAVE_DIR, unique_filename)
        image.save(image_filepath)
        app.logger.info(f"Image saved at: {image_filepath}")

        # 이미지 URL 생성 (예: http://<server_ip>:<port>/images/<filename>)
        image_url = f"server_ip/images/{unique_filename}"
        app.logger.info(f"Image URL: {image_url}")

        return jsonify({"imageUrl": image_url, "filename": unique_filename})
    except torch.cuda.OutOfMemoryError:
        torch.cuda.empty_cache()
        app.logger.error("CUDA out of memory error")
        return jsonify({"error": "CUDA out of memory. Try reducing the batch size or freeing up memory."}), 500
    except Exception as e:
        app.logger.error(f"Error generating image: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/resize', methods=['POST'])
def resize_image():
    data = request.get_json()
    platform = data.get('platform')
    filename = data.get('filename')

    try:
        # 생성된 이미지 파일 경로
        image_filepath = os.path.join(IMAGE_SAVE_DIR, filename)
        
        # 이미지를 열기
        img = Image.open(image_filepath)

        if platform == 1:  # YouTube, Blog
            width = 1080
            height = 720
        elif platform == 2:  # Instagram (4:5)
            width = 1080
            height = 1350
        elif platform == 3:  # Instagram (1:1)
            width = 1080
            height = 1080

        minsize = min(width, height)
        img_resize = img.resize((minsize, minsize), Image.LANCZOS)
        img_bg = img.resize((width, height), Image.LANCZOS)
        img_bg = img_bg.filter(ImageFilter.GaussianBlur(radius=30))
        img_bg.paste(img_resize, ((width - img_resize.width) // 2, (height - img_resize.height) // 2))
        
        resized_filename = f"resized_{platform}_{filename}"
        resized_filepath = os.path.join(IMAGE_SAVE_DIR, resized_filename)
        img_out_rgb = img_bg.convert('RGB')
        img_out_rgb.save(resized_filepath, quality=100)
        app.logger.info(f"Resized image saved at: {resized_filepath}")

        return jsonify({"imageUrl": f"server_ip/images/{resized_filename}"})
    except Exception as e:
        app.logger.error(f"Error resizing image: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/images/<filename>')
def get_image(filename):
    file_path = os.path.join(IMAGE_SAVE_DIR, filename)
    app.logger.info(f"Serving image from: {file_path}")
    return send_file(file_path, mimetype='image/png')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=server_port)
