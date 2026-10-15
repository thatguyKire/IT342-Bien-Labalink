import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import MachinePage from './pages/MachinePage';
import BookingPage from './pages/BookingPage';
import TransactionsPage from './pages/TransactionsPage';
import StaffPage from './pages/StaffPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import PaymentResultPage from './pages/PaymentResultPage';
import { getSession } from './lib/session';

/**
 * UX guard only: the server enforces roles on every API call, so bypassing
 * this in the browser gains nothing.
 */
function AttendantRoute({ children }: { children: React.ReactNode }) {
  const session = getSession();
  if (!session) {
    return <Navigate to="/login" replace />;
  }
  if (session.role !== 'ATTENDANT') {
    return <Navigate to="/login?error=not_attendant" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/machines" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
        <Route path="/payment/success"
               element={<PaymentResultPage outcome="success" />} />
        <Route path="/payment/cancel"
               element={<PaymentResultPage outcome="cancel" />} />

        <Route path="/machines" element={
          <AttendantRoute><MachinePage /></AttendantRoute>
        } />
        <Route path="/bookings" element={
          <AttendantRoute><BookingPage /></AttendantRoute>
        } />
        <Route path="/transactions" element={
          <AttendantRoute><TransactionsPage /></AttendantRoute>
        } />
        <Route path="/staff" element={
          <AttendantRoute><StaffPage /></AttendantRoute>
        } />

        {/* Old routes */}
        <Route path="/dashboard" element={<Navigate to="/machines" replace />} />
        <Route path="/wallet" element={<Navigate to="/transactions" replace />} />
        <Route path="*" element={<Navigate to="/machines" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
