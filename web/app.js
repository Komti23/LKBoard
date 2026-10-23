/**
 * LKBoard Web Showcase & Interactive Logic
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Tab switching in preview window
  const tabButtons = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.tab-content');

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const tabTarget = btn.getAttribute('data-tab');

      tabButtons.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const activeContent = document.getElementById(`tab-${tabTarget}`);
      if (activeContent) {
        activeContent.classList.add('active');
      }
    });
  });

  // 2. Interactive To-Do Checkboxes on the sticky notes demo
  const todoItems = document.querySelectorAll('.todo-item');
  todoItems.forEach(item => {
    const checkbox = item.querySelector('input[type="checkbox"]');
    if (checkbox) {
      checkbox.addEventListener('change', (e) => {
        if (checkbox.checked) {
          item.classList.add('done');
        } else {
          item.classList.remove('done');
        }
      });
      item.addEventListener('click', (e) => {
        if (e.target !== checkbox) {
          checkbox.checked = !checkbox.checked;
          checkbox.dispatchEvent(new Event('change'));
        }
      });
    }
  });

  // 3. Platform Detection
  const isWindows = navigator.platform.indexOf('Win') > -1 || navigator.userAgent.indexOf('Windows') > -1;
  const winHighlight = document.getElementById('dl-win-card');
  if (winHighlight && isWindows) {
    winHighlight.classList.add('highlight');
  }

  // 4. Smooth Anchor Scrolling
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      const targetId = this.getAttribute('href');
      if (targetId.length > 1) {
        const targetElement = document.querySelector(targetId);
        if (targetElement) {
          e.preventDefault();
          targetElement.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
          });
        }
      }
    });
  });

  // 5. Query Live App Status from relay server
  fetch('/api/status')
    .then(r => r.json())
    .then(data => {
      const statusPill = document.getElementById('server-status-pill');
      if (statusPill && data.status === 'online') {
        statusPill.innerHTML = `🟢 Облачный сервер онлайн · ${data.roomsCount || 0} комнат`;
      }
    })
    .catch(() => {});
});
