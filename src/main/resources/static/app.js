// Простой фронтенд для REST API /api/v1/...
// Токен JWT после входа хранится в localStorage и отправляется в заголовке Authorization.

const API = '/api/v1';

const state = {
  token: localStorage.getItem('token'),
  email: localStorage.getItem('email'),
  roles: JSON.parse(localStorage.getItem('roles') || '[]'),
  currentStudentId: null,
};

const $ = (id) => document.getElementById(id);

function hasRole(role) {
  return state.roles.includes('ROLE_' + role);
}

function showMessage(text, isError = false) {
  const box = $('message');
  box.textContent = text;
  box.classList.toggle('message_error', isError);
  box.hidden = false;
  clearTimeout(showMessage.timer);
  showMessage.timer = setTimeout(() => { box.hidden = true; }, 4000);
}

// Общая функция запроса к бэкенду
async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) headers.Authorization = 'Bearer ' + state.token;

  const response = await fetch(API + path, { ...options, headers });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!response.ok) {
    if (response.status === 401 && state.token) logout();
    const reason = (data && (data.message || data.error)) || '';
    const byStatus = {
      400: 'Неверные данные',
      401: 'Нужно войти заново',
      403: 'Нет доступа (неверный логин/пароль или не хватает прав)',
      404: 'Не найдено',
    };
    throw new Error(`${byStatus[response.status] || 'Ошибка сервера'} (${response.status}) ${reason}`.trim());
  }
  return data;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ));
}

// ---------- Вход / выход ----------

$('loginForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  try {
    const data = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: form.email.value, password: form.password.value }),
    });
    state.token = data.token;
    state.email = form.email.value;
    state.roles = data.roles;
    localStorage.setItem('token', state.token);
    localStorage.setItem('email', state.email);
    localStorage.setItem('roles', JSON.stringify(state.roles));
    render();
    showMessage('Вы вошли как ' + state.email);
  } catch (error) {
    showMessage(error.message, true);
  }
});

$('logoutButton').addEventListener('click', logout);

function logout() {
  state.token = null;
  state.email = null;
  state.roles = [];
  localStorage.removeItem('token');
  localStorage.removeItem('email');
  localStorage.removeItem('roles');
  render();
}

// ---------- Отрисовка по ролям ----------

function render() {
  const loggedIn = Boolean(state.token);
  const canSeeStudents = hasRole('ADMIN') || hasRole('TEACHER');
  const isAdmin = hasRole('ADMIN');
  const isSuperAdmin = hasRole('SUPER_ADMIN');

  $('loginSection').hidden = loggedIn;
  $('userBox').hidden = !loggedIn;
  $('userInfo').textContent = loggedIn
    ? `${state.email} (${state.roles.map((r) => r.replace('ROLE_', '')).join(', ')})`
    : '';

  $('studentsSection').hidden = !loggedIn || !canSeeStudents;
  $('usersSection').hidden = !loggedIn || !isSuperAdmin;
  $('studentDetails').hidden = true;
  document.querySelectorAll('.admin-only').forEach((el) => { el.hidden = !isAdmin; });

  if (loggedIn && canSeeStudents) loadStudents();
  if (loggedIn && isSuperAdmin) loadUsers();
}

// ---------- Студенты ----------

async function loadStudents() {
  try {
    const data = await api('/student');
    $('studentsCount').textContent = `(${data.totalCount})`;
    const isAdmin = hasRole('ADMIN');
    $('studentsTable').innerHTML = data.response.map((s) => `
      <tr>
        <td>${s.id}</td>
        <td>${escapeHtml(s.name)}</td>
        <td>${escapeHtml(s.surname)}</td>
        <td>
          <button class="button button_light button_small" data-open="${s.id}">Подробнее</button>
          ${isAdmin ? `<button class="button button_danger button_small" data-delete="${s.id}">Удалить</button>` : ''}
        </td>
      </tr>`).join('') || '<tr><td colspan="4">Студентов пока нет</td></tr>';
  } catch (error) {
    showMessage(error.message, true);
  }
}

$('reloadStudents').addEventListener('click', loadStudents);

$('studentsTable').addEventListener('click', async (event) => {
  const openId = event.target.dataset.open;
  const deleteId = event.target.dataset.delete;
  if (openId) openStudent(openId);
  if (deleteId && confirm('Удалить студента с ID ' + deleteId + '?')) {
    try {
      await api('/student/' + deleteId, { method: 'DELETE' });
      showMessage('Студент удалён');
      if (String(state.currentStudentId) === deleteId) $('studentDetails').hidden = true;
      loadStudents();
    } catch (error) {
      showMessage(error.message, true);
    }
  }
});

async function openStudent(id) {
  try {
    const s = await api('/student/' + id);
    state.currentStudentId = s.id;
    $('detailsTitle').textContent = `${s.name} ${s.surname}`;
    $('detailsList').innerHTML = `
      <dt>ID</dt><dd>${s.id}</dd>
      <dt>Имя</dt><dd>${escapeHtml(s.name)}</dd>
      <dt>Фамилия</dt><dd>${escapeHtml(s.surname)}</dd>
      <dt>Телефон</dt><dd>${escapeHtml(s.phoneNumber) || '—'}</dd>
      <dt>Email</dt><dd>${escapeHtml(s.email) || '—'}</dd>`;
    const form = $('editStudentForm');
    form.phoneNumber.value = s.phoneNumber || '';
    form.email.value = s.email || '';
    $('studentDetails').hidden = false;
    $('studentDetails').scrollIntoView({ behavior: 'smooth' });
  } catch (error) {
    showMessage(error.message, true);
  }
}

$('closeDetails').addEventListener('click', () => { $('studentDetails').hidden = true; });

$('addStudentForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  try {
    const s = await api('/student/save', {
      method: 'POST',
      body: JSON.stringify({
        name: form.name.value,
        surname: form.surname.value,
        phone_number: form.phone_number.value || null,
        email: form.email.value || null,
      }),
    });
    form.reset();
    showMessage(`Добавлен студент ${s.name} ${s.surname} (ID ${s.id})`);
    loadStudents();
  } catch (error) {
    showMessage(error.message, true);
  }
});

$('editStudentForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  try {
    await api('/student/' + state.currentStudentId, {
      method: 'PUT',
      body: JSON.stringify({
        phoneNumber: form.phoneNumber.value || null,
        email: form.email.value || null,
      }),
    });
    showMessage('Контакты сохранены');
    openStudent(state.currentStudentId);
  } catch (error) {
    showMessage(error.message, true);
  }
});

// ---------- Пользователи ----------

async function loadUsers() {
  try {
    const data = await api('/users');
    $('usersCount').textContent = `(${data.totalCount})`;
    $('usersTable').innerHTML = data.responses.map((u) => `
      <tr>
        <td>${u.id}</td>
        <td>${escapeHtml(u.email)}</td>
        <td>${escapeHtml(u.roles.join(', '))}</td>
        <td>${u.email === state.email ? '' : `<button class="button button_danger button_small" data-delete="${u.id}">Удалить</button>`}</td>
      </tr>`).join('');
  } catch (error) {
    showMessage(error.message, true);
  }
}

$('reloadUsers').addEventListener('click', loadUsers);

$('usersTable').addEventListener('click', async (event) => {
  const id = event.target.dataset.delete;
  if (id && confirm('Удалить пользователя с ID ' + id + '?')) {
    try {
      await api('/users/' + id, { method: 'DELETE' });
      showMessage('Пользователь удалён');
      loadUsers();
    } catch (error) {
      showMessage(error.message, true);
    }
  }
});

$('addUserForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.target;
  try {
    const u = await api('/users/save', {
      method: 'POST',
      body: JSON.stringify({ email: form.email.value, password: form.password.value, roles: [form.role.value] }),
    });
    form.reset();
    showMessage('Добавлен пользователь ' + u.email);
    loadUsers();
  } catch (error) {
    showMessage(error.message, true);
  }
});

render();
