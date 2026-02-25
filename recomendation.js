import express from 'express';

const router = express.Router();

// Get recommendations
router.get('/', (req, res) => {
  const recommendations = [
    { id: 1, title: 'GitHub Projects Integration', description: 'Enable board view to better track lagging tasks.', category: 'Platform' },
    { id: 2, title: 'Pair Programming Session', description: 'Amit Singh is lagging; schedule a pair coding session.', category: 'Feedback' },
    { id: 3, title: 'Strict PR Guidelines', description: 'Implement smaller PRs to reduce code review delay.', category: 'Workflow' },
    { id: 4, title: 'Daily Standup Sync', description: 'Improve communication for contributors with high delays.', category: 'Workflow' },
  ];
  res.json(recommendations);
});

export default router;
