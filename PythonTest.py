#!/usr/bin/env python3

# -*- coding: utf-8 -*-

import asyncio
import websockets
import json
import sys
import threading
import time
from datetime import datetime

class IRCWebSocketClient:
    def __init__(self, server_uri):
        self.server_uri = server_uri
        self.websocket = None
        self.username = None
        self.running = False

    async def connect(self, username):
        """连接到WebSocket服务器"""
        try:
            self.websocket = await websockets.connect(self.server_uri)
            self.username = username
            self.running = True
            print(f"成功连接到服务器: {self.server_uri}")
            
            # 发送加入消息
            join_msg = {
                "type": "join",
                "username": username
            }
            await self.websocket.send(json.dumps(join_msg))
            
            # 启动接收消息的协程
            await self.receive_messages()
            
        except Exception as e:
            print(f"连接服务器失败: {e}")
            return False

    async def receive_messages(self):
        """接收并处理来自服务器的消息"""
        try:
            while self.running:
                message = await self.websocket.recv()
                await self.handle_message(message)
        except websockets.exceptions.ConnectionClosed:
            print("与服务器的连接已断开")
            self.running = False
        except Exception as e:
            print(f"接收消息时出错: {e}")

    async def handle_message(self, message):
        """处理接收到的消息"""
        try:
            data = json.loads(message)
            msg_type = data.get('type', '')
            
            if msg_type == 'welcome':
                print(f"[系统] {data.get('message', '')}")
                users = data.get('users', [])
                if users:
                    print(f"[系统] 当前在线用户: {', '.join(users)}")
                    
            elif msg_type == 'message':
                timestamp = data.get('timestamp', '')
                username = data.get('username', 'Anonymous')
                msg = data.get('message', '')
                print(f"[{timestamp}] {username}: {msg}")
                
            elif msg_type == 'user_joined':
                timestamp = data.get('timestamp', '')
                username = data.get('username', 'Anonymous')
                print(f"[{timestamp}] [系统] {username} 加入了聊天室")
                
            elif msg_type == 'user_left':
                timestamp = data.get('timestamp', '')
                username = data.get('username', 'Anonymous')
                print(f"[{timestamp}] [系统] {username} 离开了聊天室")
                
            elif msg_type == 'private_message':
                timestamp = data.get('timestamp', '')
                sender = data.get('from', 'Anonymous')
                msg = data.get('message', '')
                print(f"[{timestamp}] [私聊] {sender}: {msg}")
                
            elif msg_type == 'private_message_sent':
                target = data.get('to', '')
                msg = data.get('message', '')
                print(f"[私聊已发送] 致 {target}: {msg}")
                
            elif msg_type == 'minecraft_command':
                timestamp = data.get('timestamp', '')
                username = data.get('username', 'Anonymous')
                command = data.get('command', '')
                print(f"[{timestamp}] [命令] {username}: {command}")
                
            elif msg_type == 'error':
                error_msg = data.get('message', '未知错误')
                print(f"[错误] {error_msg}")
                
            else:
                print(f"[未知消息] {message}")
                
        except json.JSONDecodeError:
            print(f"[原始消息] {message}")
        except Exception as e:
            print(f"处理消息时出错: {e}")

    async def send_message(self, message):
        """发送公共消息"""
        if not self.websocket or not self.running:
            print("未连接到服务器")
            return
            
        try:
            msg_data = {
                "type": "message",
                "message": message
            }
            await self.websocket.send(json.dumps(msg_data))
        except Exception as e:
            print(f"发送消息失败: {e}")

    async def send_private_message(self, target, message):
        """发送私聊消息"""
        if not self.websocket or not self.running:
            print("未连接到服务器")
            return
            
        try:
            msg_data = {
                "type": "private_message",
                "target": target,
                "message": message
            }
            await self.websocket.send(json.dumps(msg_data))
        except Exception as e:
            print(f"发送私聊消息失败: {e}")

    async def send_minecraft_command(self, command):
        """发送Minecraft命令"""
        if not self.websocket or not self.running:
            print("未连接到服务器")
            return
            
        try:
            msg_data = {
                "type": "minecraft_command",
                "command": command
            }
            await self.websocket.send(json.dumps(msg_data))
        except Exception as e:
            print(f"发送命令失败: {e}")

    async def disconnect(self):
        """断开与服务器的连接"""
        self.running = False
        if self.websocket:
            await self.websocket.close()
            print("已断开与服务器的连接")

def show_help():
    """显示帮助信息"""
    print("\n=== 命令帮助 ===")
    print("/help - 显示此帮助信息")
    print("/pm <用户名> <消息> - 发送私聊消息")
    print("/cmd <命令> - 发送Minecraft命令")
    print("/quit - 退出客户端")
    print("直接输入文本 - 发送公共消息")
    print("================\n")

async def main():
    """主函数"""
    server_uri = "ws://156.238.232.145:8080"
    
    # 获取用户名
    username = input("请输入用户名: ").strip()
    if not username:
        print("用户名不能为空")
        return
    
    # 创建客户端实例
    client = IRCWebSocketClient(server_uri)
    
    # 连接到服务器
    try:
        # 启动连接任务
        connect_task = asyncio.create_task(client.connect(username))
        
        # 等待一小段时间确保连接建立
        await asyncio.sleep(1)
        
        if not client.running:
            print("无法连接到服务器")
            return
        
        print("\n连接成功! 输入 /help 查看可用命令")
        show_help()
        
        # 主循环处理用户输入
        while client.running:
            try:
                # 获取用户输入
                user_input = await asyncio.get_event_loop().run_in_executor(None, input, "> ")
                
                if not user_input:
                    continue
                    
                # 处理命令
                if user_input.startswith('/'):
                    parts = user_input.split(' ', 2)
                    command = parts[0].lower()
                    
                    if command == '/help':
                        show_help()
                        
                    elif command == '/pm' and len(parts) >= 3:
                        target = parts[1]
                        message = parts[2]
                        await client.send_private_message(target, message)
                        
                    elif command == '/cmd' and len(parts) >= 2:
                        command_text = parts[1]
                        await client.send_minecraft_command(command_text)
                        
                    elif command == '/quit':
                        await client.disconnect()
                        break
                        
                    else:
                        print("无效命令或参数不足。输入 /help 查看帮助")
                        
                else:
                    # 发送公共消息
                    await client.send_message(user_input)
                    
            except KeyboardInterrupt:
                print("\n收到中断信号，正在断开连接...")
                await client.disconnect()
                break
            except Exception as e:
                print(f"处理输入时出错: {e}")
                
    except Exception as e:
        print(f"客户端运行出错: {e}")
    finally:
        await client.disconnect()

if __name__ == "__main__":
    print("IRC WebSocket 客户端")
    print("连接地址:", "ws://156.238.232.145:8080")
    
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n客户端已退出")
    except Exception as e:
        print(f"程序出错: {e}")