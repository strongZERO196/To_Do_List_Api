<template>
  <div id="app">
    <h1>To-Do List</h1>
    <div class="flex-container">
      <input v-model="newTask" @keyup.enter="addTask" placeholder="タスクを追加">
      <input v-model="searchQuery" placeholder="検索ワードを入力">
      <input v-model="userInput" placeholder="おまかせ" @keyup.enter="sendToChatGPT">
    </div>
    <ul>
      <!-- v-forディレクティブでfilteredTasksを使用してフィルタリングされたタスクを表示 -->
      <li v-for="group in filteredTasks" :key="group.date">
        <h2>{{ formatDateTime(group.date) }}</h2>

        <ul>
          <li v-for="task in group.tasks" :key="task.id">
            <div class="task-container">
              <label :class="{ completed: task.completed }">
                <input type="checkbox" v-model="task.completed" @change="updateTask(task)">
                {{ task.title }}
              </label>
              <button @click="removeTask(task)">Delete</button>
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>
<script>
export default {
  data() {
    return {
      newTask: '',
      tasks: [],
      searchQuery: '',
      userInput: ''
    };
  },
  created() {
    this.fetchTasks();
  },
  methods: {
    fetchTasks() {
      this.axios.get('http://localhost:8080/getAllTasks')
        .then(response => {
          this.tasks = this.groupTasksByDate(response.data);
          console.log(this.tasks);
        })
        .catch(error => {
          console.error("Error fetching tasks:", error);
        });
    },

    groupTasksByDate(tasks) {
      const grouped = {};
      tasks.forEach(task => {
        // JSTに変換するために `convertToJST` 関数を使用
        const jstDate = this.convertToJST(task.createdAt);
        // 日付のみを取得するために `formatDateTime` 関数を使用
        const taskDate = this.formatDateTime(jstDate);

        if (!grouped[taskDate]) {
          grouped[taskDate] = [];
        }
        // タスクを適切な日付のグループに追加
        grouped[taskDate].push(task);
      });
      return Object.keys(grouped).map(date => ({
        date,
        tasks: grouped[date]
      }));
    },


    formatDateTime(dateTime) {
      if (!dateTime) return '';
      const date = this.convertToJST(dateTime);
      // 時間部分を含めず、日付だけを返すようにフォーマットを修正
      return date.getFullYear() + '-' +
        ('0' + (date.getMonth() + 1)).slice(-2) + '-' +
        ('0' + date.getDate()).slice(-2);
    },

    convertToLocalTime(utcDateString) {
      if (!utcDateString) return '';
      const date = new Date(utcDateString);
      // getTimezoneOffsetはミリ秒単位でタイムゾーンの差を返すため、60000で割って分単位に直す
      return new Date(date.getTime() - (date.getTimezoneOffset() * 60000));
    },

    convertToJST(utcDateString) {
      if (!utcDateString) return '';
      // 既にJSTであると仮定して、Dateオブジェクトを作成
      const jstDate = new Date(utcDateString);
      return jstDate;
    },

    addTask(taskDescription = '') {
      taskDescription = (typeof taskDescription === 'string') ? taskDescription : '';
      const newTask = (typeof this.newTask === 'string') ? this.newTask : '';
      const taskTitle = (taskDescription || newTask).trim();
      if (!taskTitle) {
        return;
      }
      const task = {
        title: taskTitle,
        completed: false,
      };

      this.axios.post('http://localhost:8080/createTask', task)
        .then(response => {
          // JSTに変換し、日付のみを取得
          const jstDate = this.convertToJST(response.data.createdAt);
          const taskDate = this.formatDateTime(jstDate);

          const group = this.tasks.find(g => g.date === taskDate);
          if (group) {
            group.tasks.push(response.data);
          } else {
            this.tasks.push({ date: taskDate, tasks: [response.data] });
          }
        })
        .catch(error => {
          console.error("Error adding task:", error);
        });
      this.newTask = '';
    },

    updateTask(task) {
      this.axios.put(`http://localhost:8080/updateTask/${task.id}`, task)
        .catch(error => {
          console.error("Error updating task:", error);
        });
    },
    removeTask(task) {
      this.axios.delete(`http://localhost:8080/deleteTask/${task.id}`)
        .then(() => {
          let emptyGroupIndex = null;
          this.tasks.forEach((group, index) => {
            group.tasks = group.tasks.filter(t => t.id !== task.id);
            if (group.tasks.length === 0) {
              emptyGroupIndex = index; // 空のグループのインデックスを記録
            }
          });

          // タスクが空になった日付のグループを削除します
          if (emptyGroupIndex !== null) {
            this.tasks.splice(emptyGroupIndex, 1);
          }
        })
        .catch(error => {
          console.error("Error deleting task:", error);
        });
    },

    sendToChatGPT() {
      // ユーザーの入力に基づいてタスクリストを作成するための指示を含めたプロンプト
      const inputPrompt = `ユーザーが次のように要求しています: "${this.userInput}"。これに基づいて、詳細なタスクリストを作成してください。
      また文章の作成についてのルールを記載します。
      ・出力するタスクリストの内容を「|」区切りでレスポンスを返却してください。（例:朝をおきる|朝ごはん食べる|…など）
      ・タスク内容以外の文章は記載しないでください。「タスクリストの内容: 」「以上のようなタスクリストが作成されます。」などの余計な文章は不要なので絶対につけないこと。
      ・出力内容は15文字以内としてください。
      #出力`;

      const payload = {
        model: "gpt-3.5-turbo", // 適切なモデルを選択
        messages: [
          { role: "system", content: "以下の指示に従ってタスクリストを作成します。" },
          { role: "user", content: inputPrompt } // ユーザーの要求を含むプロンプト
        ]
      };
      const headers = {
        'Authorization': `Bearer XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`, // APIキーを読み込む(APIキーは各自取得して設定)
        'Content-Type': 'application/json'
      };

      this.axios.post('https://api.openai.com/v1/chat/completions', payload, { headers })
        .then(response => {
          // "|"で区切られた応答を受け取り、それぞれのタスクを登録する
          const responseContent = response.data.choices[0].message.content.trim();
          console.log(responseContent);

          this.registerTasksFromResponse(responseContent);
          this.userInput = '';
        })
        .catch(error => {
          console.error("Error sending to ChatGPT:", error);
        });
    },
    registerTasksFromResponse(responseContent) {
      // "|"で区切り、それぞれのタスクを登録する
      const tasks = responseContent.split('|').map(task => task.trim()).filter(task => task !== '');
      tasks.forEach(task => {
        this.addTask(task); // 既存のタスク追加メソッドを呼び出して、各タスクを登録
      });
    },

    // 全角文字を半角文字に変換する関数
    toHalfWidth(str) {
      return str.replace(/[Ａ-Ｚａ-ｚ０-９]/g, function (s) {
        return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
      });
    },
    // 半角文字を全角文字に変換する関数（必要であれば）
    toFullWidth(str) {
      return str.replace(/[A-Za-z0-9]/g, function (s) {
        return String.fromCharCode(s.charCodeAt(0) + 0xFEE0);
      });
    },
  },
  computed: {
    filteredTasks() {
      if (!this.searchQuery) {
        return this.tasks;
      }
      let normalizedQuery = this.toHalfWidth(this.searchQuery.toLowerCase());
      return this.tasks.map(group => ({
        date: group.date,
        tasks: group.tasks.filter(task =>
          this.toHalfWidth(task.title.toLowerCase()).includes(normalizedQuery))
      })).filter(group => group.tasks.length > 0);
    }
  },


};
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap');

/* 完了したタスクに適用されるスタイル。打ち消し線を追加します */
.completed {
  text-decoration: line-through;
}

body {
  font-family: 'Roboto', sans-serif;
  /* フォントを変更 */
  background-color: #f7f7f7;
  /* 背景色を軽いグレーに変更 */
}

#app {
  max-width: 600px;
  margin: auto;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  /* シャドウを軽くする */
  background-color: #fff;
  /* 背景色を白に設定 */
  border-radius: 8px;
  /* 角を丸くする */

}

h1 {
  color: #333;
}

.flex-container {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  /* フレックスコンテナのマージンを追加 */
}

input[type="text"] {
  width: calc(100% / 3 - 10px);
  /* 3つの入力フィールドのために幅を3で割り、隙間を引く */
  padding: 12px;
  margin-bottom: 15px;
  margin-right: 10px;
  /* 最後の要素以外に適用 */
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
  background-color: #f8f8f8;
  font-size: 16px;
}

/* 最後のinput要素のみ、margin-rightを0にする */
input[type="text"]:last-child {
  margin-right: 0;
}

/* レスポンシブ対応 */
@media (max-width: 768px) {
  .flex-container {
    flex-direction: column;
  }

  input[type="text"] {
    width: 100%;
    /* モバイルビューではフル幅 */
    margin-right: 0;
    /* 縦並びのためマージンは不要 */
  }
}


input[type="text"]:focus {
  border-color: #4A90E2;
  /* フォーカス時のボーダー色を変更する */
  outline: none;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.2);
  /* フォーカス時に影を追加 */

}

/* プレースホルダーのスタイルを設定する */
::placeholder {
  color: #aaa;
  /* プレースホルダーの色を設定する */
}


ul {
  list-style: none;
  padding: 0;
}

li {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 5px;
}

label {
  margin-right: 10px;
}

button {
  background-color: #f44336;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 5px;
  cursor: pointer;
  margin-left: auto;
  /* 必要に応じて、ボタンを右側に押し出します */
  transition: background-color 0.3s;
  /* 背景色のトランジションを追加 */

}

.task-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* タスクのテキストと削除ボタンを中央揃えにします */
}


button:hover {
  background-color: #d32f2f;
  /* ホバー時の背景色 */
  transition: background-color 0.2s;
  /* トランジションの時間を調整 */
}

.completed {
  color: #bbb;
  text-decoration: line-through;
}

@media (max-width: 768px) {
  #app {
    padding: 10px;
  }

  input[type="text"] {
    margin-bottom: 15px;
  }
}
</style>
